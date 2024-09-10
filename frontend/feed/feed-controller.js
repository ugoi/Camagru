import { getUserFeed } from "./feed-model.js";
import { checkFileType } from "../upload/upload-model.js";

// Load user media
window.addEventListener("DOMContentLoaded", async (event) => {
  await reloadFeed();
});

document
  .getElementById("loadMoreButton")
  .addEventListener("click", async (event) => {
    await loadNextFeed();
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

  console.log("Json", json);

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
      outputMedia.src = objectURL;
      outputMedia.controls = true;
      outputMedia.className = "captured-media";

      const videoInteractions = document.createElement("div");
      videoInteractions.innerHTML = `
    <div>
      <button>Like</button>
      <form action="/action_page.php">
        <input type="text" id="lname" name="lname" value="Add a comment..." />
        <input type="submit" value="Submit" />
      </form>
    </div>
      `;

      const divWrapper = document.createElement("div");
      divWrapper.style.paddingBottom = "10px";
      divWrapper.id = mediaElement.id;
      divWrapper.appendChild(outputMedia);
      divWrapper.appendChild(videoInteractions);
      myMedia.appendChild(divWrapper);
    }
  }
}
