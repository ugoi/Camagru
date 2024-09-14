import {
  checkIsVideo,
  postMedia,
  getServeMedia,
  mediaService,
  publishMedia,
  getUserMedia,
  checkFileType,
  deleteMedia,
} from "./upload-model.js";

/**
 * @type {HTMLVideoElement}
 */
var videoPreview = document.querySelector("#videoElement");

/**
 * @type {HTMLDivElement}
 */
var cameraPreview = document.querySelector("#cameraPreview");

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
  const videoButton = document.querySelector("#videoButton");
  if (value) {
    videoButton.innerText = "Stop recording";
  } else {
    videoButton.innerText = "Start recording";
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
  /**
   * @type {HTMLImageElement}
   */
  const outputPhoto = document.querySelector("#image");

  /**
   * @type {HTMLVideoElement}
   */
  const outputVideo = document.querySelector("#outputVideo");

  if (value == null) {
    cameraPreview.style.display = "";
    outputPhoto.style.display = "none";
    outputPhoto.src = "";
    outputVideo.style.display = "none";
    outputVideo.src = "";
  } else {
    cameraPreview.style.display = "none";
    var isVideo = checkIsVideo(value);
    if (isVideo) {
      outputPhoto.style.display = "none";
      outputPhoto.src = "";
      outputVideo.style.display = "";
      outputVideo.src = URL.createObjectURL(value);
    } else {
      outputVideo.style.display = "none";
      outputVideo.src = "";
      outputPhoto.style.display = "";
      outputPhoto.src = URL.createObjectURL(value);
    }
  }
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
  const publishButton = document.getElementById("publishButton");
  if (value == null) {
    publishButton.style.display = "none";
  } else {
    publishButton.style.display = "";
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
      let element = document.createElement("img");
      element.src = path;
      element.className = "carousel-img";
      document.getElementById("logos-slide").appendChild(element);

      element.addEventListener("click", async (event) => {
        const result = await fetch(path);
        const blob = await result.blob();
        setOverlayMedia(blob);
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
      console.log("Something went wrong with the camera... Try restarting the fronternd server" + error);
      console.log(error);
    });
}

var after = null;

async function reloadUserMedia() {
  after = null;
  const myMedia = document.getElementById("myMedia");
  myMedia.innerHTML = "";
  await loadNextUserMedia();
}

/**
 * @param {String} after
 * @returns {Promise<void>}
 */
async function loadNextUserMedia() {
  const myMedia = document.getElementById("myMedia");
  const result = await getUserMedia(after, 3);
  const json = await result.json();
  const data = json.data;
  after = json.paging.after;
  userMedia = data;
  if (after == null) {
    document.getElementById("loadMoreButton").style.display = "none";
  } else {
    document.getElementById("loadMoreButton").style.display = "";
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
      divWrapper.style.paddingBottom = "10px";
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

// Load user media
window.addEventListener("DOMContentLoaded", async (event) => {
  reloadUserMedia();
});

document.getElementById("loadMoreButton").addEventListener("click", (event) => {
  loadNextUserMedia();
});

//Event listeners
document.getElementById("fileInput").addEventListener("change", (event) => {
  if (event?.target?.files && event.target.files[0]) {
    var blob = event.target.files[0]; // See step 1 above
    setMedia(blob);
  }
});

document
  .getElementById("overlayFileInput")
  .addEventListener("change", (event) => {
    if (event?.target?.files && event.target.files[0]) {
      var blob = event.target.files[0];
      setOverlayMedia(blob);
    }
  });

document
  .getElementById("publishButton")
  .addEventListener("click", async (event) => {
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

document
  .getElementById("submitButton")
  .addEventListener("click", async (event) => {
    event.preventDefault();
    let formData = new FormData();
    formData.append("media", media);
    formData.append("overlayMedia", overlayMedia);
    var postResponse = await postMedia(formData);
    var postJson = await postResponse.json();
    var id = postJson.containerId;
    setContainerId(id);
    var response = await getServeMedia(id);
    var myBlob = await response.blob();
    setMedia(myBlob);
  });

document.getElementById("cancelButton").addEventListener("click", (event) => {
  event.preventDefault();
  setContainerId(null);
  setMedia(null);
});

//Stop camera preview. This should be done when the user leaves the page.
window.addEventListener("beforeunload", () => {
  if (videoPreview.srcObject) {
    let stream = videoPreview.srcObject;
    let tracks = stream.getTracks();

    tracks.forEach((track) => {
      track.stop();
    });

    videoPreview.srcObject = null;
  }
});

document.querySelector("#videoButton").addEventListener(
  "click",
  async (ev) => {
    if (isRecording == false) {
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

document.querySelector("#photoButton").addEventListener(
  "click",
  async (ev) => {
    const blob = await mediaService.takepicture(videoPreview.srcObject);
    setMedia(blob);
    ev.preventDefault();
  },
  false
);
