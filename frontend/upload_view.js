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
 * @type {HTMLCanvasElement}
 */
var canvas = document.querySelector("#canvas");

fileInput.addEventListener("change", (event) => {
  if (event?.target?.files && event.target.files[0]) {
    cameraPreview.style.display = "none";
    photo.style.display = "";
    photo.src = URL.createObjectURL(event.target.files[0]);
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

  if (overlayMediaFile.size != 0) {
    console.log("appedning overlayMedia");

    formData.append("overlayMedia", overlayMediaFile);
  }

  if (mediaFile.size != 0) {
    console.log("appedning media");

    // formData.append("media", mediaFile);
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
  photo.src = objectURL;
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
