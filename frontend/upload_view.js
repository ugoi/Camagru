import { postMedia, getMedia } from "./upload.js";

/**
 * @type {HTMLFormElement}
 * */
const mediaForm = document.getElementById("mediaForm");

/**
 * @type {HTMLFormElement}
 */
const overlayMeidaForm = document.getElementById("overlayMediaForm");

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

/**
 * @type {HTMLCanvasElement}
 */
var canvas = document.querySelector("#canvas");

var isRecording = false;

let stopRecordingFunction;

fileInput.addEventListener("change", (event) => {
  if (event?.target?.files && event.target.files[0]) {
    var blob = event.target.files[0]; // See step 1 above
    console.log("blob type: ", blob.type);
    /**
     * @type {String}
     */
    cameraPreview.style.display = "none";
    var isVideo = checkIfVideo(blob);
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

const formData = new FormData();

submitButton.addEventListener("click", async (event) => {
  event.preventDefault();
  cameraPreview.style.display = "display";
  const overlayMediaFormData = new FormData(overlayMeidaForm);
  const mediaFormData = new FormData(mediaForm);

  var overlayMediaFile = overlayMediaFormData.get("overlayMedia");
  var mediaFile = mediaFormData.get("media");

  console.log("overlayMediaFile", overlayMediaFile);
  console.log("mediaFile", mediaFile);

  if (overlayMediaFile.size != 0) {
    console.log("appedning overlayMedia");

    formData.append("overlayMedia", overlayMediaFile);
  }

  if (mediaFile.size != 0) {
    console.log("appedning media");

    formData.append("media", mediaFile);
  }

  // Display the key/value pairs
  console.log("Sart logging");
  for (var pair of formData.entries()) {
    console.log(pair[0] + ", " + pair[1]);
  }

  var postResponse = await postMedia(formData);

  var postJson = await postResponse.json();

  var id = postJson.containerId;

  var response = await getMedia(id);

  var mimeType = await response.headers.get("Content-type");

  var myBlob = await response.blob();

  var objectURL = URL.createObjectURL(myBlob);

  var isVideo = checkIfVideo(myBlob);

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

if (navigator.mediaDevices.getUserMedia) {
  navigator.mediaDevices
    .getUserMedia({ video: true })
    .then(function (stream) {
      video.srcObject = stream;
      video.play();
    })
    .catch(function (err0r) {
      console.log("Something went wrong!");
    });
}

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
      var result = startRecording(video.srcObject, 5000);
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
      stopRecordingFunction();
      // stopRecording(video.srcObject);

      isRecording = false;
      videoButton.innerText = "Start recording";
    }
    ev.preventDefault();
  },
  false
);

photoButton.addEventListener(
  "click",
  (ev) => {
    takepicture();
    ev.preventDefault();
  },
  false
);

function takepicture() {
  /**
   * @type {CanvasRenderingContext2D}
   */
  const context = canvas.getContext("2d");

  var width = video.getBoundingClientRect().width;
  var height = video.getBoundingClientRect().height;

  canvas.width = width;
  canvas.height = height;

  context.drawImage(video, 0, 0, width, height);

  const data = canvas.toDataURL("image/png");

  photo.setAttribute("src", data);
  cameraPreview.style.display = "none";
  photo.style.display = "";
  const overlayMediaFormData = new FormData(overlayMeidaForm);

  canvas.toBlob((blob) => {
    var mediaFile = blob;
    saveBlob(mediaFile, "test.jpeg");
    formData.append("media", mediaFile);
  });
}

var saveBlob = (function () {
  var a = document.createElement("a");
  document.body.appendChild(a);
  a.style = "display: none";
  return function (blob, fileName) {
    var url = window.URL.createObjectURL(blob);
    a.href = url;
    a.download = fileName;
    a.click();
    window.URL.revokeObjectURL(url);
  };
})();

/**
 *
 * @param {Blob} blob
 * @returns {Boolean} if the blob is a video
 */
function checkIfVideo(blob) {
  var isVideo = false;
  var mimeType = blob.type;

  if (mimeType.startsWith("video")) {
    isVideo = true;
  } else {
    isVideo = false;
  }
  return isVideo;
}

function startRecording(stream, lengthInMS) {
  let recorder = new MediaRecorder(stream);
  let data = [];

  recorder.ondataavailable = (event) => data.push(event.data);
  recorder.start();

  console.log("recording");

  let stopped = new Promise((resolve, reject) => {
    recorder.onstop = resolve;
    recorder.onerror = (event) => reject(event.name);
  });

  // Usage Example:
  const timeout = createCompletableTimeout(() => {
    console.log("Timeout completed");

    if (recorder.state === "recording") {
      recorder.stop();
    }
  }, lengthInMS);

  let recorded = timeout;

  stopRecordingFunction = () => {
    console.log("about to call stopRecordingFunction");
    timeout.complete();
  };

  return Promise.all([stopped, recorded]).then(() => data);
}

function stopRecording(stream) {
  stream.getTracks().forEach((track) => track.stop());
}

function wait(delayInMS) {
  return new Promise((resolve) => setTimeout(resolve, delayInMS));
}

function createCompletableTimeout(callback, delay) {
  let timeoutId;
  let isCompleted = false;
  let resolvePromise;

  // Create a promise that will resolve when the timeout completes or is resolved manually
  const completablePromise = new Promise((resolve) => {
    resolvePromise = resolve; // Capture the resolve function
    timeoutId = setTimeout(() => {
      if (!isCompleted) {
        callback();
        resolve(); // Resolve the promise when the timeout completes naturally
      }
    }, delay);
  });

  // Function to complete the timeout before it naturally resolves
  completablePromise.complete = () => {
    if (!isCompleted) {
      clearTimeout(timeoutId);
      callback(); // Optionally call the callback when completing
      resolvePromise(); // Resolve the promise manually
      isCompleted = true;
    }
  };

  return completablePromise;
}
