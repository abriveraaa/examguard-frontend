from flask import Flask, request, jsonify
import cv2
import mediapipe as mp
import numpy as np

VERSION = "1.0.0"

app = Flask(__name__)

mp_face_mesh = mp.solutions.face_mesh

face_mesh = mp_face_mesh.FaceMesh(
    static_image_mode=True,
    max_num_faces=3,
    refine_landmarks=True,
    min_detection_confidence=0.5,
    min_tracking_confidence=0.5
)

LEFT_EYE = 33
RIGHT_EYE = 263
NOSE = 1


@app.route("/health", methods=["GET"])
def health():
    return jsonify({
        "status": "ok",
        "service": "mediapipe-face",
        "version": VERSION
    })


@app.route("/analyze", methods=["POST"])
def analyze():
    if "image" not in request.files:
        return jsonify({
            "available": False,
            "version": VERSION,
            "message": "No image uploaded."
        }), 400

    uploaded_file = request.files["image"]

    image_bytes = np.frombuffer(uploaded_file.read(), np.uint8)
    frame = cv2.imdecode(image_bytes, cv2.IMREAD_COLOR)

    if frame is None:
        return jsonify({
            "available": False,
            "version": VERSION,
            "message": "Invalid image."
        }), 400

    rgb = cv2.cvtColor(frame, cv2.COLOR_BGR2RGB)
    results = face_mesh.process(rgb)

    if not results.multi_face_landmarks:
        return jsonify({
            "available": True,
            "version": VERSION,
            "facePresent": False,
            "faceCount": 0,
            "multipleFaces": False,
            "lookingAway": False,
            "lookingDown": False,
            "yaw": 0.0,
            "pitch": 0.0,
            "message": "No face detected."
        })

    face_count = len(results.multi_face_landmarks)
    first_face = results.multi_face_landmarks[0]

    height, width, _ = frame.shape

    xs = [lm.x for lm in first_face.landmark]
    ys = [lm.y for lm in first_face.landmark]

    box = {
        "x": float(min(xs)),
        "y": float(min(ys)),
        "width": float(max(xs) - min(xs)),
        "height": float(max(ys) - min(ys))
    }

    landmarks = {
        "nose": {
            "x": float(first_face.landmark[1].x),
            "y": float(first_face.landmark[1].y)
        },
        "leftEye": {
            "x": float(first_face.landmark[33].x),
            "y": float(first_face.landmark[33].y)
        },
        "rightEye": {
            "x": float(first_face.landmark[263].x),
            "y": float(first_face.landmark[263].y)
        },
        "mouth": {
            "x": float(first_face.landmark[13].x),
            "y": float(first_face.landmark[13].y)
        }
    }

    nose = first_face.landmark[NOSE]
    left_eye = first_face.landmark[LEFT_EYE]
    right_eye = first_face.landmark[RIGHT_EYE]

    eye_center_x = (left_eye.x + right_eye.x) / 2.0
    eye_center_y = (left_eye.y + right_eye.y) / 2.0

    yaw = float(nose.x - eye_center_x)
    pitch = float(nose.y - eye_center_y)

    looking_away = abs(yaw) > 0.08
    looking_down = pitch > 0.12

    return jsonify({
        "available": True,
        "version": VERSION,
        "facePresent": True,
        "faceCount": face_count,
        "multipleFaces": face_count > 1,
        "lookingAway": looking_away,
        "lookingDown": looking_down,
        "yaw": yaw,
        "pitch": pitch,
        "message": "MediaPipe face analysis completed.",
        "box": box,
        "landmarks": landmarks,
    })



if __name__ == "__main__":
    app.run(
        host="127.0.0.1",
        port=5005,
        debug=False
    )