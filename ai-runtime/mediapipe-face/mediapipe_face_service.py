from flask import Flask, request, jsonify
import cv2
import mediapipe as mp
import numpy as np

VERSION = "1.2.0"

app = Flask(__name__)

mp_face_mesh = mp.solutions.face_mesh

face_mesh = mp_face_mesh.FaceMesh(
    static_image_mode=True,
    max_num_faces=3,
    refine_landmarks=True,
    min_detection_confidence=0.6,
    min_tracking_confidence=0.6
)

NOSE = 1
CHIN = 152
MOUTH_CENTER = 13
UPPER_LIP = 13
LOWER_LIP = 14
LEFT_MOUTH = 61
RIGHT_MOUTH = 291

LEFT_EYE_OUTER = 33
LEFT_EYE_INNER = 133
RIGHT_EYE_INNER = 362
RIGHT_EYE_OUTER = 263

LEFT_EYE_TOP = 159
LEFT_EYE_BOTTOM = 145
RIGHT_EYE_TOP = 386
RIGHT_EYE_BOTTOM = 374

LEFT_IRIS = [468, 469, 470, 471, 472]
RIGHT_IRIS = [473, 474, 475, 476, 477]


@app.route("/health", methods=["GET"])
def health():
    return jsonify({
        "status": "ok",
        "service": "mediapipe-face",
        "version": VERSION
    })


def avg_point(face, indexes):
    xs = [face.landmark[i].x for i in indexes]
    ys = [face.landmark[i].y for i in indexes]
    return float(sum(xs) / len(xs)), float(sum(ys) / len(ys))


def clamp(value, minimum, maximum):
    return max(minimum, min(maximum, value))


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

    gray = cv2.cvtColor(frame, cv2.COLOR_BGR2GRAY)

    brightness = float(np.mean(gray))
    blur_score = float(cv2.Laplacian(gray, cv2.CV_64F).var())

    too_dark = brightness < 45
    too_bright = brightness > 235
    too_blurry = blur_score < 18

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
            "lookingUp": False,
            "eyeGazeAway": False,
            "eyeGazeDown": False,
            "eyeGazeUp": False,
            "eyeGazeNotCentered": False,
            "faceTooClose": False,
            "faceTooFar": False,
            "faceNotCentered": False,
            "warning": False,
            "redViolation": True,
            "suspicious": True,
            "severity": "MAJOR",
            "yaw": 0.0,
            "pitch": 0.0,
            "gazeX": 0.0,
            "gazeY": 0.0,
            "message": "No face detected.",
            "brightness": brightness,
            "blurScore": blur_score,
            "tooDark": too_dark,
            "tooBright": too_bright,
            "tooBlurry": too_blurry
        })

    face_count = len(results.multi_face_landmarks)
    first_face = results.multi_face_landmarks[0]

    xs = [lm.x for lm in first_face.landmark]
    ys = [lm.y for lm in first_face.landmark]

    box_x = float(clamp(min(xs), 0.0, 1.0))
    box_y = float(clamp(min(ys), 0.0, 1.0))
    box_w = float(clamp(max(xs) - min(xs), 0.0, 1.0))
    box_h = float(clamp(max(ys) - min(ys), 0.0, 1.0))

    box = {
        "x": box_x,
        "y": box_y,
        "width": box_w,
        "height": box_h
    }

    box_center_x = box_x + box_w / 2.0
    box_center_y = box_y + box_h / 2.0

    nose = first_face.landmark[NOSE]
    mouth = first_face.landmark[MOUTH_CENTER]
    chin = first_face.landmark[CHIN]

    left_eye_outer = first_face.landmark[LEFT_EYE_OUTER]
    left_eye_inner = first_face.landmark[LEFT_EYE_INNER]
    right_eye_inner = first_face.landmark[RIGHT_EYE_INNER]
    right_eye_outer = first_face.landmark[RIGHT_EYE_OUTER]

    eye_center_x = (left_eye_outer.x + right_eye_outer.x) / 2.0
    eye_center_y = (left_eye_outer.y + right_eye_outer.y) / 2.0

    yaw = float(nose.x - eye_center_x)
    pitch = float(nose.y - eye_center_y)

    left_iris_x, left_iris_y = avg_point(first_face, LEFT_IRIS)
    right_iris_x, right_iris_y = avg_point(first_face, RIGHT_IRIS)

    left_eye_width = max(0.0001, left_eye_inner.x - left_eye_outer.x)
    right_eye_width = max(0.0001, right_eye_outer.x - right_eye_inner.x)

    left_iris_ratio_x = (left_iris_x - left_eye_outer.x) / left_eye_width
    right_iris_ratio_x = (right_iris_x - right_eye_inner.x) / right_eye_width

    gaze_x = float(((left_iris_ratio_x - 0.5) + (right_iris_ratio_x - 0.5)) / 2.0)

    left_eye_center_y = (
                                first_face.landmark[LEFT_EYE_TOP].y +
                                first_face.landmark[LEFT_EYE_BOTTOM].y
                        ) / 2.0

    right_eye_center_y = (
                                 first_face.landmark[RIGHT_EYE_TOP].y +
                                 first_face.landmark[RIGHT_EYE_BOTTOM].y
                         ) / 2.0

    left_eye_height = max(
        0.0001,
        abs(first_face.landmark[LEFT_EYE_TOP].y - first_face.landmark[LEFT_EYE_BOTTOM].y)
    )

    right_eye_height = max(
        0.0001,
        abs(first_face.landmark[RIGHT_EYE_TOP].y - first_face.landmark[RIGHT_EYE_BOTTOM].y)
    )

    left_gaze_y = (left_iris_y - left_eye_center_y) / left_eye_height
    right_gaze_y = (right_iris_y - right_eye_center_y) / right_eye_height

    gaze_y = float((left_gaze_y + right_gaze_y) / 2.0)

    # ==========================
    # FACE OBSTRUCTION CHECKS
    # ==========================

    left_eye_opening = abs(
        first_face.landmark[LEFT_EYE_TOP].y -
        first_face.landmark[LEFT_EYE_BOTTOM].y
    )

    right_eye_opening = abs(
        first_face.landmark[RIGHT_EYE_TOP].y -
        first_face.landmark[RIGHT_EYE_BOTTOM].y
    )

    avg_eye_opening = float((left_eye_opening + right_eye_opening) / 2.0)

    mouth_opening = abs(
        first_face.landmark[UPPER_LIP].y -
        first_face.landmark[LOWER_LIP].y
    )

    mouth_width = abs(
        first_face.landmark[RIGHT_MOUTH].x -
        first_face.landmark[LEFT_MOUTH].x
    )

    eyes_probably_covered = avg_eye_opening < 0.006

    mouth_center_visible = (
            box_x <= mouth.x <= box_x + box_w and
            box_y <= mouth.y <= box_y + box_h
    )

    mouth_probably_covered = not mouth_center_visible

    face_partially_obstructed = (
            eyes_probably_covered or
            mouth_probably_covered
    )

    multiple_faces = face_count > 1

    face_too_close = box_w > 0.56 or box_h > 0.72
    face_too_far = box_w < 0.10 or box_h < 0.14

    face_not_centered = (
            box_center_x < 0.20 or
            box_center_x > 0.80 or
            box_center_y < 0.08 or
            box_center_y > 0.84
    )

    head_turned_left_right = abs(yaw) > 0.050
    # head_looking_down = pitch > 0.160
    # head_looking_up = pitch < 0.050
    #
    # eye_gaze_away = abs(gaze_x) > 0.055
    # eye_gaze_down = gaze_y > 0.100
    # eye_gaze_up = gaze_y < -0.120

    head_looking_down = (
            not face_too_close and
            pitch > 0.105
    )

    head_looking_up = (
            not face_too_close and
            pitch < 0.045
    )

    eye_gaze_away = abs(gaze_x) > 0.050

    eye_gaze_down = gaze_y > 0.045

    eye_gaze_up = gaze_y < -0.110

    eye_gaze_not_centered = (
            eye_gaze_away or
            eye_gaze_down or
            eye_gaze_up
    )

    looking_away = head_turned_left_right
    looking_down = head_looking_down
    looking_up = head_looking_up

    red_violation = (
            multiple_faces or
            (
                    not face_too_close and
                    not face_too_far and
                    not face_not_centered and
                    (
                            looking_away or
                            looking_down
                    )
            )
    )

    warning = (
            not red_violation and
            (
                    eye_gaze_not_centered or
                    face_too_close or
                    face_too_far or
                    face_not_centered or
                    too_dark or
                    too_bright or
                    too_blurry or
                    eyes_probably_covered or
                    mouth_probably_covered or
                    face_partially_obstructed
            )
    )

    suspicious = red_violation or warning

    if multiple_faces:
        severity = "CRITICAL"
        message = "Multiple faces detected."
    elif face_too_close:
        severity = "WARNING"
        message = "Face is too close to the camera. Please move slightly backward."
    elif face_too_far:
        severity = "WARNING"
        message = "Face is too far from the camera. Please move closer."
    elif face_not_centered:
        severity = "WARNING"
        message = "Face is not centered. Please sit properly and keep your face in the camera view."
    elif looking_away:
        severity = "MAJOR"
        message = f"Head turned left/right. yaw={yaw:.3f}, gazeX={gaze_x:.3f}"
    elif looking_down:
        severity = "MAJOR"
        message = f"Head looking down. pitch={pitch:.3f}, gazeY={gaze_y:.3f}"
    elif looking_up:
        severity = "MAJOR"
        message = f"Head looking up. pitch={pitch:.3f}, gazeY={gaze_y:.3f}"
    elif too_dark:
        severity = "WARNING"
        message = "Camera view is too dark. Improve lighting."
    elif too_bright:
        severity = "WARNING"
        message = "Camera view is too bright. Reduce glare or backlight."
    elif too_blurry:
        severity = "WARNING"
        message = "Camera view is blurry. Adjust the camera."
    elif eyes_probably_covered:
        severity = "WARNING"
        message = "Eyes are not clearly visible."
    elif mouth_probably_covered:
        severity = "WARNING"
        message = "Lower face may be covered."
    elif face_partially_obstructed:
        severity = "WARNING"
        message = "Face may be partially obstructed."
    elif eye_gaze_not_centered:
        severity = "WARNING"
        message = f"Eye gaze not centered. gazeX={gaze_x:.3f}, gazeY={gaze_y:.3f}"
    elif face_too_far:
        severity = "WARNING"
        message = "Please adjust your camera position."
    elif face_too_close:
        severity = "WARNING"
        message = "Face is too close. Move slightly backward."
    elif face_not_centered:
        severity = "WARNING"
        message = "Face is not centered. Adjust camera position."
    else:
        severity = "OK"
        message = "Face monitoring state is normal."

    landmarks = {
        "nose": {
            "x": float(nose.x),
            "y": float(nose.y)
        },
        "leftEye": {
            "x": float(left_eye_outer.x),
            "y": float(left_eye_outer.y)
        },
        "rightEye": {
            "x": float(right_eye_outer.x),
            "y": float(right_eye_outer.y)
        },
        "mouth": {
            "x": float(mouth.x),
            "y": float(mouth.y)
        },
        "chin": {
            "x": float(chin.x),
            "y": float(chin.y)
        },
        "leftIris": {
            "x": float(left_iris_x),
            "y": float(left_iris_y)
        },
        "rightIris": {
            "x": float(right_iris_x),
            "y": float(right_iris_y)
        }
    }

    return jsonify({
        "available": True,
        "version": VERSION,
        "facePresent": True,
        "faceCount": face_count,
        "multipleFaces": multiple_faces,

        "lookingAway": looking_away,
        "lookingDown": looking_down,
        "lookingUp": looking_up,

        "eyeGazeAway": eye_gaze_away,
        "eyeGazeDown": eye_gaze_down,
        "eyeGazeUp": eye_gaze_up,
        "eyeGazeNotCentered": eye_gaze_not_centered,

        "faceTooClose": face_too_close,
        "faceTooFar": face_too_far,
        "faceNotCentered": face_not_centered,

        "warning": warning,
        "redViolation": red_violation,
        "suspicious": suspicious,
        "severity": severity,

        "yaw": yaw,
        "pitch": pitch,
        "gazeX": gaze_x,
        "gazeY": gaze_y,

        "message": message,

        "box": box,
        "landmarks": landmarks,
        "brightness": brightness,
        "blurScore": blur_score,
        "tooDark": too_dark,
        "tooBright": too_bright,
        "tooBlurry": too_blurry,

        "eyesProbablyCovered": eyes_probably_covered,
        "mouthProbablyCovered": mouth_probably_covered,
        "facePartiallyObstructed": face_partially_obstructed,
        "avgEyeOpening": avg_eye_opening,
        "mouthOpening": float(mouth_opening),
        "mouthWidth": float(mouth_width)
    })


if __name__ == "__main__":
    app.run(
        host="127.0.0.1",
        port=5005,
        debug=False
    )