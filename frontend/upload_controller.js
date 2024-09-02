import {
  checkIsVideo,
  postMedia,
  getServeMedia,
  mediaService,
} from "./upload_model.js";

/**
 * @type {HTMLFormElement}
 * */
const mediaForm = document.getElementById("mediaForm");

/**
 * @type {HTMLFormElement}
 */
const overlayMediaForm = document.getElementById("overlayMediaForm");

/**
 * @type {HTMLButtonElement}
 */
const submitButton = document.getElementById("submitButton");

/**
 * @type {HTMLButtonElement}
 */
const cancelButton = document.getElementById("cancelButton");

/**
 * @type {HTMLInputElement}
 */
const fileInput = document.getElementById("fileInput");

/**
 * @type {HTMLInputElement}
 */
const overlayFileInput = document.getElementById("overlayFileInput");

/**
 * @type {HTMLImageElement}
 */
const photo = document.querySelector("#image");

/**
 * @type {HTMLVideoElement}
 */
const outputVideo = document.querySelector("#outputVideo");

/**
 * @type {HTMLVideoElement}
 */
var video = document.querySelector("#videoElement");

/**
 * @type {HTMLDivElement}
 */
var cameraPreview = document.querySelector("#cameraPreview");

/**
 * @type {HTMLButtonElement}
 */
var photoButton = document.querySelector("#photoButton");

/**
 * @type {HTMLButtonElement}
 */
var videoButton = document.querySelector("#videoButton");

const formData = new FormData();

var isRecording = false;

var overlayAssetPaths = [
  "assets/overlay/clownfish.png",
  "assets/overlay/goat.png",
  "assets/overlay/police-armor.png",
  "assets/overlay/red-rocket.png",
];

// Add overlay assets to the carousel
if (overlayAssetPaths && overlayAssetPaths.length > 0) {
  for (let index = 0; index < overlayAssetPaths.length; index++) {
    let path = overlayAssetPaths[index];
    let element = document.createElement("img");
    element.src = path;
    element.className = "carousel-img";
    document.getElementById("logos-slide").appendChild(element);

    element.addEventListener("click", async (event) => {
      const result = await fetch(path);
      const blob = await result.blob();
      console.log(blob);
      formData.delete("overlayMedia");
      formData.append("overlayMedia", blob);
      var fileInput = document.getElementById("overlayFileInput");
      fileInput.value = "";
    });
  }
} else {
  console.log("No overlay assets found");
}

//Start camera preview
if (navigator.mediaDevices.getUserMedia) {
  navigator.mediaDevices
    .getUserMedia({ video: true })
    .then(function (stream) {
      video.srcObject = stream;
      video.play();
    })
    .catch(function (error) {
      console.log("Something went wrong!");
      console.log(error);
    });
}

// Load user media

//Event listeners
fileInput.addEventListener("change", (event) => {
  if (event?.target?.files && event.target.files[0]) {
    var blob = event.target.files[0]; // See step 1 above
    formData.delete("media");
    formData.append("media", blob);
    console.log("blob type: ", blob.type);
    /**
     * @type {String}
     */
    cameraPreview.style.display = "none";
    var isVideo = checkIsVideo(blob);
    console.log("isVideo", isVideo);

    if (isVideo) {
      photo.style.display = "none";
      outputVideo.style.display = "";
      outputVideo.src = URL.createObjectURL(event.target.files[0]);
    } else {
      outputVideo.style.display = "none";
      photo.style.display = "";
      photo.src = URL.createObjectURL(event.target.files[0]);
    }
  }
});

overlayFileInput.addEventListener("change", (event) => {
  if (event?.target?.files && event.target.files[0]) {
    var blob = event.target.files[0];
    /**
     * @type {String}
     */
    formData.delete("overlayMedia");
    formData.append("overlayMedia", blob);
  }
});

submitButton.addEventListener("click", async (event) => {
  event.preventDefault();
  cameraPreview.style.display = "display";

  if (overlayMediaForm == null || mediaForm == null) {
    throw new Error("Overlay media form or media form is null");
  }

  const overlayMediaFormData = new FormData(overlayMediaForm);
  const mediaFormData = new FormData(mediaForm);

  var overlayMediaFile = overlayMediaFormData.get("overlayMedia");
  var mediaFile = mediaFormData.get("media");

  console.log("overlayMediaFile", overlayMediaFile);
  console.log("mediaFile", mediaFile);

  // Display the key/value pairs
  console.log("Sart logging");
  for (var pair of formData.entries()) {
    console.log(pair[0] + ", " + pair[1]);
  }

  var postResponse = await postMedia(formData);

  var postJson = await postResponse.json();

  var id = postJson.containerId;

  var response = await getServeMedia(id);

  var mimeType = await response.headers.get("Content-type");

  var myBlob = await response.blob();

  var objectURL = URL.createObjectURL(myBlob);

  var isVideo = checkIsVideo(myBlob);

  if (isVideo) {
    outputVideo.src = objectURL;
  } else {
    photo.src = objectURL;
  }
});

cancelButton.addEventListener("click", (event) => {
  event.preventDefault();
  /**
   * @type {HTMLInputElement}
   */
  var fileInput = document.getElementById("fileInput");
  fileInput.value = "";

  photo.src = "";
  photo.style.display = "none";

  outputVideo.src = "";
  outputVideo.style.display = "none";

  cameraPreview.style.display = "";
});

//Stop camera preview. This should be done when the user leaves the page.
window.addEventListener("beforeunload", () => {
  if (video.srcObject) {
    let stream = video.srcObject;
    let tracks = stream.getTracks();

    tracks.forEach((track) => {
      track.stop();
    });

    video.srcObject = null;
  }
});

videoButton.addEventListener(
  "click",
  async (ev) => {
    if (isRecording == false) {
      var result = mediaService.startRecording(video.srcObject, 5000);
      isRecording = true;
      videoButton.innerText = "Stop recording";
      var recordedChunks = await result;
      console.log("Recording finished");

      let recordedBlob = new Blob(recordedChunks, { type: "video/mp4" });
      formData.append("media", recordedBlob);
      var url = URL.createObjectURL(recordedBlob);
      console.log("url", url);

      outputVideo.src = url;
      outputVideo.style.display = "";
      cameraPreview.style.display = "none";
      videoButton.innerText = "Start recording";
    } else {
      console.log("about to call stop3");
      mediaService.stopRecording();
      isRecording = false;
      videoButton.innerText = "Start recording";
    }
    ev.preventDefault();
  },
  false
);

photoButton.addEventListener(
  "click",
  async (ev) => {
    const blob = await mediaService.takepicture(video.srcObject);
    var objectURL = URL.createObjectURL(blob);

    photo.setAttribute("src", objectURL);
    cameraPreview.style.display = "none";
    photo.style.display = "";
    formData.append("media", blob);
    ev.preventDefault();
  },
  false
);
