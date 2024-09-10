import { getUserFeed } from "./feed-model.js";
import { checkFileType } from "../upload/upload-model.js";

// Load user media
window.addEventListener("DOMContentLoaded", async (event) => {
  reloadFeed();
});

document.getElementById("loadMoreButton").addEventListener("click", (event) => {
  loadNextFeed();
});

var after = null;

async function reloadFeed() {
  after = null;
  const myMedia = document.getElementById("myMedia");
  myMedia.innerHTML = "";
  try {
    await loadNextFeed();
  } catch (error) {
    console.log("loadNextFeed() returned exception");
  }
}

/**
 * @param {String} after
 * @returns {Promise<void>}
 */
async function loadNextFeed() {
  const myMedia = document.getElementById("myMedia");
  const result = await getUserFeed(after, 3);
  const json = await result.json();
  const data = json.data;
  after = json.paging.after;

  console.log("After: ", after);

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
      divWrapper.style.paddingBottom = "10px";
      divWrapper.id = mediaElement.id;
      outputMedia.src = objectURL;
      outputMedia.controls = true;
      outputMedia.className = "captured-media";
      divWrapper.appendChild(outputMedia);
      myMedia.appendChild(divWrapper);
    }
  }
}
