import {
  deleteLike,
  getLikesCount,
  getUserFeed,
  postLike,
} from "./feed-model.js";
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
  const mediaCollection = document.getElementById("myMedia");
  mediaCollection.innerHTML = "";
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
  const mediaCollection = document.getElementById("myMedia");
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
      const mediaId = mediaElement.id;

      const fileType = await checkFileType(downloadUrl);
      const isVideo = fileType === "video";
      const objectURL = downloadUrl;

      const mediaWrapper = document.createElement("div");
      mediaWrapper.className = "div-wrapper";

      // Assign unique id based on the index
      const uniqueId = `collapsible-${after}-${index}`;

      // Get likes and comments
      /**
       * @type {number}
       */
      let totalLikes = 0;

      {
        const result = await getLikesCount(mediaId);

        const json = await result.json();

        totalLikes = Number(json.total_count);
      }

      mediaWrapper.innerHTML = `
        <div class="div-wrapper">
        ${
          isVideo
            ? `
          <video src="${objectURL}" controls class="captured-media"></video>
        `
            : `
          <img src="${objectURL}" class="captured-media" />
        `
        }
          <div class="media-actions">
            <div class="like-section">
              <button class="like-btn" id=like-btn-${mediaId}>❤️</button>
              <span class="like-count" id=like-count-${mediaId}>${totalLikes}</span>
              <!-- Replace 10 with dynamic count -->
            </div>
            <form action="/action_page.php" class="comment-form">
              <input
                type="text"
                id="comment"
                name="comment"
                placeholder="Add a comment..."
                class="comment-input"
              />
              <input type="submit" value="Submit" class="btn" />
            </form>
          </div>

          <div class="wrap-collabsible">
            <input id="${uniqueId}" class="toggle" type="checkbox" />
            <label for="${uniqueId}" class="lbl-toggle">Show comments</label>
            <div class="collapsible-content">
              <div class="content-inner">
                <div class="comment">
                  <p><strong>Stefan:</strong> Looks very nice</p>
                </div>
                <div class="comment">
                  <p><strong>Bob:</strong> Where is this?</p>
                </div>
              </div>
            </div>
          </div>
        </div>
      `;

      mediaCollection.appendChild(mediaWrapper);

      const likeButton = document.getElementById(`like-btn-${mediaId}`);
      const likeCount = document.getElementById(`like-count-${mediaId}`);
      likeButton.addEventListener("click", async (event) => {
        console.log("LIKE BUTTON CLICKED");
        console.log("mediaId", mediaId);

        try {
          await postLike(mediaId);
          const newCount = Number(likeCount.innerHTML) + 1;
          likeCount.innerHTML = `${newCount}`;
        } catch (error) {
          try {
            deleteLike(mediaId);
            const newCount = Number(likeCount.innerHTML) - 1;
            likeCount.innerHTML = `${newCount}`;
          } catch (error) {
            console.log("Failed to delete like");
            console.log(error);
          }
        }
      });
    }
  }
}
