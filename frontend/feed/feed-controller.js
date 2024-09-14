import {
  deleteLike,
  getComments,
  getLikesCount,
  getUserFeed,
  postComment,
  postLike,
} from "./feed-model.js";
import { checkUserAuthentication } from "../services/auth-service.js";

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
  } catch (error) {}
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
  if (after == null) {
    document.getElementById("loadMoreButton").style.display = "none";
  } else {
    document.getElementById("loadMoreButton").style.display = "";
  }

  if (data && data.length > 0) {
    // Get media details concurretnly
    let commentsResJsonArray = [];
    let totalLikesArray = [];

    await Promise.all(
      data.map(async (mediaElement) => {
        // State variables
        const isAuth = checkUserAuthentication();
        const mediaId = mediaElement.id;
        if (isAuth) {
          const [likesResult, commentRes] = await Promise.all([
            getLikesCount(mediaId),
            getComments(mediaId),
          ]);
          var commentsResJson = await commentRes.json();
          var totalLikes = Number((await likesResult.json()).total_count);
          commentsResJsonArray.push(commentsResJson);
          totalLikesArray.push(totalLikes);
        }
      })
    );

    data.forEach((mediaElement) => {
      // State variables
      const isAuth = checkUserAuthentication();
      const mediaId = mediaElement.id;
      const commentsResJson = commentsResJsonArray.shift();
      const totalLikes = totalLikesArray.shift();
      const isVideo = mediaElement.mime_type.includes("video") ? true : false;
      const objectURL = mediaElement.downloadUrl;

      // Create the media element
      const mediaWrapper = document.createElement("div");
      mediaWrapper.className = "div-wrapper";
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

        ${
          isAuth
            ? `
            <div class="media-actions">
            <div class="like-section">
              <button class="like-btn" id=like-btn-${mediaId}>❤️</button>
              <span class="like-count" id=like-count-${mediaId}>${totalLikes}</span>
              <!-- Replace 10 with dynamic count -->
            </div>
            <form action="/action_page.php" class="comment-form" id="comment-form-${mediaId}">
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
            <input id="${`collapsible-${mediaId}`}" class="toggle" type="checkbox" />
            <label for="${`collapsible-${mediaId}`}" class="lbl-toggle">Show comments</label>
            <div class="collapsible-content">
              <div class="content-inner">
                ${commentsResJson.data
                  .map((comment) => {
                    return `<div class="comment"> <p><strong>${comment.username}:</strong> ${comment.comment_body}</p> </div>`;
                  })
                  .join("")}
              </div>
            </div>
          </div>
          `
            : ``
        }
        </div>
      `;

      mediaCollection.appendChild(mediaWrapper);

      // Add event listeners

      if (isAuth) {
        const likeButton = document.getElementById(`like-btn-${mediaId}`);
        const likeCount = document.getElementById(`like-count-${mediaId}`);
        likeButton.addEventListener("click", async (event) => {
          try {
            await postLike(mediaId);
            const newCount = Number(likeCount.innerHTML) + 1;
            likeCount.innerHTML = `${newCount}`;
          } catch (error) {
            try {
              console.clear();
              await deleteLike(mediaId);
              const newCount = Number(likeCount.innerHTML) - 1;
              likeCount.innerHTML = `${newCount}`;
            } catch (error) {
              console.log("Error while deleting like");
            }
          }
        });

        const commentForm = document.getElementById(`comment-form-${mediaId}`);
        commentForm.addEventListener("submit", async (event) => {
          event.preventDefault();
          const formData = new FormData(commentForm);
          const comment = formData.get("comment");
          await postComment(mediaId, "Default Title", comment);
        });
      }
    });
  }
}
