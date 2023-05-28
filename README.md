# GCSW IoT 23년 1학기 8조 IoT Term Project

Member : 김다은, 김승훈, 김태엽, 백민우, 박현서

Due date : 2023.06.

## App 구성과 기능 구현 형태

### Admin App 작동 방식
After approving the location-related authority from the smartphone user, various Wifi values are collected.

After bringing only the minimum information to be used for the location base among various values, receive the tag for the current location and save it in the DB in the following form.

    String RSSID, int dbm level, String position

### 데이터 수집 방식
First, data was collected based on the front door of the room, and MAC addresses and RSSI values of WiFi devices were collected in the elevator, in front of the door of the stairwell, and in the center of Artecne.

WiFi collected the top 7 based on the signal strength of the device, and when estimating the user's location, the user's WiFi data and the Euclidean distance of the DB data were compared, and the location with the smallest difference was estimated to be the same as the user's location. intend to do

### 지금까지 진행상황
Through the meeting, it was decided to express the direction from the current location to the destination through an arrow, and to express the remaining distance to the destination on the screen.

The way to mark the direction is to find the azimuth angle to the destination through the arrow, figure out which direction you are looking through the cell phone sensor, and then rotate the arrow by that angle to display the correct direction.

The method in which the remaining distance decreases according to the movement uses the method of determining whether the user is moving in the right direction through the angle of the arrow and reducing the displayed distance by the distance traveled using the gyroscope sensor and acceleration sensor.

However, since this value cannot be said to be accurate data, we plan to recalculate and update the remaining distance by re-sensing and estimating the current position at a fixed time interval.
   
### 구상한 알고리즘 및 작동방식
The algorithm to be used is the same as above, and there is no guarantee that the location will be accurately estimated. Therefore, I plan to collect data when location estimation fails and succeeds through tests in advance, and predict the accuracy of the estimation using the python library.
If this is used, the error value for the strength of the sensitivity of sensing wifi for each device will be reduced, and the ability to estimate the location will be further increased.

## Additional File Info

adminWifi.apk - Exported apk file of this project

person.db - Wifi Scanning result of IT building 4F,5F
