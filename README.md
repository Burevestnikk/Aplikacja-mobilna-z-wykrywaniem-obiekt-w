# Aplikacja-mobilna-z-wykrywaniem-obiekt-w
This is a project for Android related to the detection of objects using the device's camera. The project uses Google'S ML Kit library to process images and recognize objects on them.

When launching the application, the main menu opens, where a brief information about the project and the button to go to work with the camera are displayed. Then the permissions to use the camera are checked. If permission is already granted, the camera is opened. If the permission is not granted, the user is asked to provide access to the camera.

When you open the camera and create a preview, the object detection model is initialized. In this case, the local model "mobilenet_v1_1.0_224_quantized_1_metadata_1" is used.tflite". Then a camera capture session is initiated, in which the frames are processed using the object detection model.

The resulting frame processing results are displayed on the device screen. If objects labeled "sunglasses", "glasses" or "sunglasses" are detected in the image, they are visually indicated by rectangular frames, and information about the number of glasses detected is displayed in text form. The maximum number of detected and drawn objects is 5.

In addition, the project implements the functions of going to the main application screen and clearing previous labels and expenditures on the screen.
