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
    console.log("Loaded feed");
  } catch (error) {
    console.log("Failed to load feed", error);
  }
}

/**
 * @returns {Promise<void>}
 */
async function loadNextFeed() {
  console.log("Loading feed");

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
    let commentsResJsonDict = {};
    let totalLikesDict = {};
    let hasLikedDict = {};

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
          const json = await likesResult.json();
          var totalLikes = Number(json.total_count);
          var hasLiked = Boolean(json.has_liked);
          commentsResJsonDict[mediaId] = commentsResJson;
          totalLikesDict[mediaId] = totalLikes;
          hasLikedDict[mediaId] = hasLiked;
        }
      })
    );

    data.forEach((mediaElement) => {
      // State variables
      const isAuth = checkUserAuthentication();
      const mediaId = mediaElement.id;
      const commentsResJson = commentsResJsonDict[mediaId];
      const totalLikes = totalLikesDict[mediaId];
      let hasReallyLiked = hasLikedDict[mediaId];
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
              <div class="content-inner" id=${`comments-${mediaId}`}>
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
        const likeCountElement = document.getElementById(
          `like-count-${mediaId}`
        );
        let likeCount = Number(likeCountElement.innerHTML) || 0;
        let isLiked = hasReallyLiked;
        let locked = false;
        likeButton.addEventListener("click", async (event) => {
          console.log("Like button clicked");

          if (isLiked) {
            likeCount -= 1;
            likeCountElement.innerHTML = `${likeCount}`;
            isLiked = false;
          } else {
            likeCount += 1;
            likeCountElement.innerHTML = `${likeCount}`;
            isLiked = true;
          }

          if (!locked) {
            locked = true;
            setTimeout(async () => {
              locked = false;
              await (async () => {
                if (hasReallyLiked && isLiked === false) {
                  console.log("Deleting like");
                  try {
                    await deleteLike(mediaId);
                    hasReallyLiked = false;
                    likeCountElement.innerHTML = `${likeCount}`;
                    return;
                  } catch (error) {
                    console.error("Failed to delete like", error);
                  }
                } else if (!hasReallyLiked && isLiked === true) {
                  console.log("Adding like");
                  try {
                    await postLike(mediaId);
                    hasReallyLiked = true;
                    likeCountElement.innerHTML = `${likeCount}`;
                    return;
                  } catch (error) {
                    console.error("Failed to add like", error);
                  }
                }
              })();
            }, 1000);
          }
        });

        const commentForm = document.getElementById(`comment-form-${mediaId}`);
        commentForm.addEventListener("submit", async (event) => {
          event.preventDefault();
          const formData = new FormData(commentForm);
          const commentBody = formData.get("comment");
          if (commentBody == null || commentBody.length === 0) {
            return;
          }
          const comment = await (
            await postComment(mediaId, "Default Title", commentBody)
          ).json();

          const commentsDiv = document.getElementById(`comments-${mediaId}`);
          commentsDiv.innerHTML =
            `<div class="comment"> <p><strong>${comment.username}:</strong> ${comment.comment_body}</p> </div>` +
            commentsDiv.innerHTML;
        });
      }
    });
  }
}
