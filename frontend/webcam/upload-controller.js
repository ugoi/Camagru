import { checkUserAuthentication } from "../services/auth-service.js";
import {
  postMedia,
  getServeMedia,
  mediaService,
  publishMedia,
  getUserMedia,
  deleteMedia,
} from "./upload-model.js";

import { checkFileType, checkIsVideo } from "../utils/utils.js";

// Commonly used HTML elements
const captureImageBtn = document.getElementById("capture-image-btn");
const captureVideoBtn = document.getElementById("capture-video-btn");
/**
 * @type {HTMLVideoElement}
 */
var videoPreview = document.querySelector("#webcam");
/**
 * @type {HTMLDivElement}
 */
var snapshotImageWrapper = document.querySelector("#snapshot-image-wrapper");
/**
 * @type {HTMLDivElement}
 */
var snapshotVideoWrapper = document.querySelector("#snapshot-video-wrapper");
/**
 * @type {HTMLImageElement}
 */
const outputPhoto = document.querySelector("#snapshot-image");

/**
 * @type {HTMLVideoElement}
 */
const outputVideo = document.querySelector("#snapshot-video");

/**
 * @type {HTMLButtonElement}
 */
const closeSnapshotImageBtn = document.getElementById("close-snapshot-image");

/**
 * @type {HTMLButtonElement}
 * */
const closeSnapshotVideoBtn = document.getElementById("close-snapshot-video");

/**
 * @type {HTMLDivElement}
 * */
const customFileUpload = document.querySelector("#custom-file-upload");

/**
 * @type {HTMLInputElement}
 */
const mediaUpload = document.getElementById("media-upload");

const imageSelect = document.getElementById("image-select");

const myMedia = document.getElementById("media-div");

const submitButton = document.getElementById("submit-btn");

const publishButton = document.getElementById("publish-btn");

// State management
/**
 * @type {Boolean}
 */
var isRecording = false;
/**
 *
 * @param {Boolean} value
 */
function setIsRecording(value) {
  isRecording = value;
  renderIsRecording(value);
}
/**
 *
 * @param {Boolean} value
 */
function renderIsRecording(value) {
  if (value) {
    // Set the button to red
    captureVideoBtn.style.backgroundColor = "#f44336";
  } else {
    // Set the button to green
    captureVideoBtn.style.backgroundColor = "#4caf50";
  }
}

/**
 * @type {Blob}
 */
var media = null;
/**
 * @param {Blob} value
 * @returns {void}
 */
function setMedia(value) {
  media = value;
  renderMedia(value);
}
/**
 * @param {Blob} value
 * @returns {void}
 */
function renderMedia(value) {
  if (value == null) {
    videoPreview.style.display = "block";
    outputPhoto.style.display = "none";
    outputPhoto.src = "";
    outputVideo.style.display = "none";
    outputVideo.src = "";
    snapshotImageWrapper.style.display = "none";
    snapshotVideoWrapper.style.display = "none";
    submitButton.disabled = true;
  } else {
    videoPreview.style.display = "none";
    submitButton.disabled = false;
    var isVideo = checkIsVideo(value);
    if (isVideo) {
      outputPhoto.style.display = "none";
      outputPhoto.src = "";
      outputVideo.style.display = "block";
      outputVideo.src = URL.createObjectURL(value);
      snapshotImageWrapper.style.display = "none";
      snapshotVideoWrapper.style.display = "block";
    } else {
      outputVideo.style.display = "none";
      outputVideo.src = "";
      outputPhoto.style.display = "block";
      outputPhoto.src = URL.createObjectURL(value);
      snapshotImageWrapper.style.display = "block";
      snapshotVideoWrapper.style.display = "none";
    }
  }
}

/**
 * @type {Blob}
 */
var previewMedia = null;
/**
 * @param {Blob} value
 * @returns {void}
 */
function setPreviewMedia(value) {
  previewMedia = value;
  renderMedia(value);
}

/**
 * @type {Blob}
 */
var overlayMedia = null;
/**
 * @param {Blob} value
 * @returns {void}
 */
function setOverlayMedia(value) {
  overlayMedia = value;
}

/**
 * @type {string | null}
 */
var containerId = null;
/**
 * @param {string | null} value
 * @returns {void}
 */
function setContainerId(value) {
  containerId = value;
  renderContainerId(value);
}
/**
 * @param {string | null} value
 * @returns {void}
 */
function renderContainerId(value) {
  if (value == null) {
    publishButton.disabled = true;
  } else {
    publishButton.disabled = false;
  }
}

/**
 * @typedef {Object} ImageObject
 * @property {number} id - The unique identifier for the image.
 * @property {string} downloadUrl - The URL to download the image.
 */

/**
 * @type {Array<ImageObject>}
 */
var userMedia = [];

var after = null;

async function reloadUserMedia() {
  after = null;
  myMedia.innerHTML = "";
  await loadNextUserMedia();
}

/**
 * @param {String} after
 * @returns {Promise<void>}
 */
async function loadNextUserMedia() {
  const laodButton = document.getElementById("load-btn");
  let result;
  try {
    result = await getUserMedia(after, 3);
  } catch (error) {
    const errorMessage = error.message;
    if (errorMessage === "Failed to fetch") {
      console.log(
        "Failed to fetch. Verify the backend URL and ensure the backend is running."
      );
      alert(
        "Failed to fetch. Verify the backend URL and ensure the backend is running."
      );
      return;
    }
    return;
  }
  const json = await result.json();
  const data = json.data;
  after = json.paging.after;
  userMedia = data;

  if (after == null) {
    laodButton.style.display = "none";
  } else {
    laodButton.style.display = "block";
  }

  if (data && data.length > 0) {
    // Remove the previous images
    for (let index = 0; index < data.length; index++) {
      const mediaElement = data[index];
      const downloadUrl = mediaElement.downloadUrl;
      const fileType = await checkFileType(downloadUrl);
      const isVideo = fileType === "video";
      const objectURL = downloadUrl;
      if (isVideo) {
        var outputMedia = document.createElement("video");
      } else {
        var outputMedia = document.createElement("img");
      }
      const divWrapper = document.createElement("div");
      const deleteButton = document.createElement("button");
      deleteButton.className = "delete-media-btn";
      divWrapper.style.paddingBottom = "10px";
      divWrapper.style.display = "flex";
      divWrapper.style.flexDirection = "column";
      divWrapper.style.alignItems = "center";
      divWrapper.id = mediaElement.id;
      outputMedia.src = objectURL;
      outputMedia.controls = true;
      outputMedia.className = "captured-media";
      deleteButton.innerText = "Delete";
      deleteButton.id = "deleteButton";
      deleteButton.addEventListener("click", async (event) => {
        try {
          await deleteMedia(mediaElement.id);
          myMedia.removeChild(divWrapper);
        } catch (error) {
          console.error("Failed to delete media", error);
        }
      });
      divWrapper.appendChild(outputMedia);
      divWrapper.appendChild(deleteButton);
      myMedia.appendChild(divWrapper);
    }
  }
}

// On page load
// Load overlay assets
(async () => {
  var overlayAssetPaths = [
    "../assets/overlay/clownfish.png",
    "../assets/overlay/goat.png",
    "../assets/overlay/police-armor.png",
    "../assets/overlay/red-rocket.png",
  ];
  // Add overlay assets to the carousel
  if (overlayAssetPaths && overlayAssetPaths.length > 0) {
    for (let index = 0; index < overlayAssetPaths.length; index++) {
      let path = overlayAssetPaths[index];
      const title = path.split("/").pop();
      let element = document.createElement("option");
      element.value = path;
      element.innerHTML = title;
      imageSelect.appendChild(element);

      imageSelect.addEventListener("change", async (event) => {
        const value = event?.target?.value;
        if (value !== "") {
          captureImageBtn.disabled = false;
          captureImageBtn.style.cursor = "pointer";
          captureImageBtn.style.backgroundColor = "#4caf50";

          captureVideoBtn.disabled = false;
          captureVideoBtn.style.cursor = "pointer";
          captureVideoBtn.style.backgroundColor = "#4caf50";

          customFileUpload.style.backgroundColor = "#4caf50";
          customFileUpload.style.pointerEvents = "auto";
          mediaUpload.disabled = false;

          // Set the overlay image
          if (value === element.value) {
            const result = await fetch(path);
            const blob = await result.blob();
            setOverlayMedia(blob);
          }
        } else {
          captureImageBtn.disabled = true;
          captureImageBtn.style.cursor = "not-allowed";

          captureVideoBtn.disabled = true;
          captureVideoBtn.style.cursor = "not-allowed";

          customFileUpload.style.backgroundColor = "#d3d3d3";
          mediaUpload.disabled = true;
        }
      });
    }
  } else {
    console.log("No overlay assets found");
  }
})();

//Start camera preview
if (navigator.mediaDevices.getUserMedia) {
  navigator.mediaDevices
    .getUserMedia({ video: true })
    .then(function (stream) {
      videoPreview.srcObject = stream;
      videoPreview.play();
    })
    .catch(function (error) {
      console.log(
        "Something went wrong with the camera... Try restarting the fronternd server" +
          error
      );
      console.log(error);
    });
}

// Load user media
window.addEventListener("DOMContentLoaded", async (event) => {
  try {
    const isLoggedIn = checkUserAuthentication();
    if (!isLoggedIn) {
      window.location.href = "/login";
    }
    await reloadUserMedia();
  } catch (error) {
    console.log("Failed to load user media", error);
  }
});

document.getElementById("load-btn").addEventListener("click", (event) => {
  loadNextUserMedia();
});

//Event listeners

mediaUpload.addEventListener("change", (event) => {
  if (event?.target?.files && event.target.files[0]) {
    var blob = event.target.files[0]; // See step 1 above
    setMedia(blob);
  }
});

// document
//   .getElementById("overlayFileInput")
//   .addEventListener("change", (event) => {
//     if (event?.target?.files && event.target.files[0]) {
//       var blob = event.target.files[0];
//       setOverlayMedia(blob);
//     }
//   });

publishButton.addEventListener("click", async (event) => {
  event.preventDefault();
  if (containerId != null) {
    var response = await publishMedia(containerId);
    if (response.status === 200) {
      setContainerId(null);
      reloadUserMedia();
    } else {
      console.log("Failed to publish media");
    }
  }
});

submitButton.addEventListener("click", async (event) => {
  event.preventDefault();
  let formData = new FormData();
  if (media === null || overlayMedia === null) {
    alert("Please select both media and overlay.");
    return;
  }
  formData.append("media", media);
  formData.append("overlayMedia", overlayMedia);
  try {
    var postResponse = await postMedia(formData);
  } catch (error) {
    const errorMessage = error.message;
    if (errorMessage === "Failed to fetch") {
      console.log(
        "Failed to fetch, check the backend url and that backend is running."
      );
      return;
    }
    console.log(errorMessage);
    return;
  }
  var postJson = await postResponse.json();
  var id = postJson.containerId;
  setContainerId(id);
  try {
    var response = await getServeMedia(id);
  } catch (error) {
    console.error("Failed to get media", error);
    return;
  }
  var myBlob = await response.blob();
  setPreviewMedia(myBlob);
});

// document.getElementById("cancelButton").addEventListener("click", (event) => {
//   event.preventDefault();
//   setContainerId(null);
//   setMedia(null);
//   setPreviewMedia(null);
// });

closeSnapshotImageBtn.addEventListener("click", (event) => {
  event.preventDefault();
  setContainerId(null);
  setMedia(null);
  setPreviewMedia(null);
});

closeSnapshotVideoBtn.addEventListener("click", (event) => {
  event.preventDefault();
  // setContainerId(null);
  setMedia(null);
  setPreviewMedia(null);
});

// //Stop camera preview. This should be done when the user leaves the page.
// window.addEventListener("beforeunload", () => {
//   if (videoPreview.srcObject) {
//     let stream = videoPreview.srcObject;
//     let tracks = stream.getTracks();

//     tracks.forEach((track) => {
//       track.stop();
//     });

//     videoPreview.srcObject = null;
//   }
// });

document.querySelector("#capture-video-btn").addEventListener(
  "click",
  async (ev) => {
    if (isRecording == false) {
      setMedia(null);
      setContainerId(null);
      setPreviewMedia(null);
      var result = mediaService.startRecording(videoPreview.srcObject, 5000);
      setIsRecording(true);
      var recordedChunks = await result;
      let recordedBlob = new Blob(recordedChunks, { type: "video/mp4" });
      setMedia(recordedBlob);
      setIsRecording(false);
    } else {
      mediaService.stopRecording();
      setIsRecording(false);
    }
    ev.preventDefault();
  },
  false
);

document.querySelector("#capture-image-btn").addEventListener(
  "click",
  async (ev) => {
    const blob = await mediaService.takepicture(videoPreview.srcObject);
    setMedia(blob);
    ev.preventDefault();
  },
  false
);
