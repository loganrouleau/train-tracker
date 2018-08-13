### Train Tracker

There is a SkyTrain station outside my window. The question prompting this project was: using only my built-in laptop camera, how reliably can I record the trains coming and going?

### Dependencies

I was pleased to find that OpenCV is available for Java, but unfortunately there is no Maven artifact for it (https://github.com/opencv/opencv/issues/4588). I've tried a few alternatives with no success, so in the meantime the following dependencies will have to be manually added to the class path.

log4j-core-2.11.0.jar

log4j-api-2.11.0.jar

opencv-341.jar

opencv_java341.dll native library suitable for your OS. This project runs for me on Windows, but I cannot guarantee and would be somewhat surprised if it runs properly on Linux.

### Demo

<img src="/demo/demo.png">

I use the app by pointing my laptop at the train station. There are independent left and right cameras, which can be used to select a bounding box at the left and right ends of the platform. The two cameras are only there to confirm that if a train entered the west end of the platform heading east, it exited the east end of the platform heading east after a brief dwell time. If this data integrity check is not required, then only one camera is necessary.

There are two parameters to set. A 0-255 grayscale intensity threshold is used in a binary threshold de-noising step. A lower value means more information is retained in the image but there is a higher change for false positives. There is also a 0-100,000 motion detection tolerance which can be used to tune the sensitivity of the motion detector. This value is based on the intensity sum over all pixels of the current frame substracted by the previous frame. It's not normalized in any way so varies widely with frame size, so I use the calibration button to test it before starting a run.

All train detections for all runs are written to an output .csv file in the results directory once the app is closed. It looks something like this:

<img src="/demo/demo.jpg" width="50%">