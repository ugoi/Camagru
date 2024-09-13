import {
  deleteLike,
  getComments,
  getLikesCount,
  getUserFeed,
  postComment,
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
    // Remove the previous images

    data.forEach(async (mediaElement) => {
      console.log("Start loading media");
      const downloadUrl = mediaElement.downloadUrl;
      const mediaId = mediaElement.id;

      const likesCountPromise = getLikesCount(mediaId);
      console.log("media id: ", mediaId);

      const commentsPromise = getComments(mediaId);

      const [likesResult, commentRes] = await Promise.all([
        likesCountPromise,
        commentsPromise,
      ]);

      // let commentRes;
      // try {
      //   commentRes = await getComments(mediaId);
      //   console.log("Comments: ", commentRes);
      // } catch (error) {
      //   console.log("Error while fetching comments" + error);
      // }

      let commentsResJson = await commentRes.json();

      // const fileType = await checkFileType(downloadUrl);

      const mimeType = mediaElement.mime_type;

      console.log("Mime type: ", mimeType);

      let fileType = "image";

      if (mimeType.includes("video")) {
        fileType = "video";
      }

      const isVideo = fileType === "video";
      const objectURL = downloadUrl;

      const mediaWrapper = document.createElement("div");
      mediaWrapper.className = "div-wrapper";

      // Assign unique id based on the index
      const uniqueId = `collapsible-${mediaId}`;

      // Get likes and comments
      /**
       * @type {number}
       */
      let totalLikes = 0;

      {
        const json = await likesResult.json();

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
            <input id="${uniqueId}" class="toggle" type="checkbox" />
            <label for="${uniqueId}" class="lbl-toggle">Show comments</label>
            <div class="collapsible-content">
              <div class="content-inner">
                ${commentsResJson.data
                  .map((comment) => {
                    return `<div class="comment"> <p><strong>${comment.user_id}:</strong> ${comment.comment_body}</p> </div>`;
                  })
                  .join("")}
              </div>
            </div>
          </div>
        </div>
      `;

      mediaCollection.appendChild(mediaWrapper);

      const likeButton = document.getElementById(`like-btn-${mediaId}`);
      const likeCount = document.getElementById(`like-count-${mediaId}`);
      likeButton.addEventListener("click", async (event) => {
        try {
          await postLike(mediaId);
          const newCount = Number(likeCount.innerHTML) + 1;
          likeCount.innerHTML = `${newCount}`;
        } catch (error) {
          try {
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
        console.log(`Comment: ${comment}`);
      });

      console.log("Media loaded");
    });

    // for (let index = 0; index < data.length; index++) {
    //   console.log("Start loading media");
    //   const mediaElement = data[index];
    //   const downloadUrl = mediaElement.downloadUrl;
    //   const mediaId = mediaElement.id;

    //   const fileTypePromise = checkFileType(downloadUrl);
    //   const likesCountPromise = getLikesCount(mediaId);

    //   const [fileType, likesResult] = await Promise.all([
    //     fileTypePromise,
    //     likesCountPromise,
    //   ]);

    //   // const fileType = await checkFileType(downloadUrl);
    //   const isVideo = fileType === "video";
    //   const objectURL = downloadUrl;

    //   const mediaWrapper = document.createElement("div");
    //   mediaWrapper.className = "div-wrapper";

    //   // Assign unique id based on the index
    //   const uniqueId = `collapsible-${after}-${index}`;

    //   // Get likes and comments
    //   /**
    //    * @type {number}
    //    */
    //   let totalLikes = 0;

    //   {
    //     // const result = await getLikesCount(mediaId);

    //     const json = await likesResult.json();

    //     totalLikes = Number(json.total_count);
    //   }

    //   mediaWrapper.innerHTML = `
    //     <div class="div-wrapper">
    //     ${
    //       isVideo
    //         ? `
    //       <video src="${objectURL}" controls class="captured-media"></video>
    //     `
    //         : `
    //       <img src="${objectURL}" class="captured-media" />
    //     `
    //     }
    //       <div class="media-actions">
    //         <div class="like-section">
    //           <button class="like-btn" id=like-btn-${mediaId}>❤️</button>
    //           <span class="like-count" id=like-count-${mediaId}>${totalLikes}</span>
    //           <!-- Replace 10 with dynamic count -->
    //         </div>
    //         <form action="/action_page.php" class="comment-form">
    //           <input
    //             type="text"
    //             id="comment"
    //             name="comment"
    //             placeholder="Add a comment..."
    //             class="comment-input"
    //           />
    //           <input type="submit" value="Submit" class="btn" />
    //         </form>
    //       </div>

    //       <div class="wrap-collabsible">
    //         <input id="${uniqueId}" class="toggle" type="checkbox" />
    //         <label for="${uniqueId}" class="lbl-toggle">Show comments</label>
    //         <div class="collapsible-content">
    //           <div class="content-inner">
    //             <div class="comment">
    //               <p><strong>Stefan:</strong> Looks very nice</p>
    //             </div>
    //             <div class="comment">
    //               <p><strong>Bob:</strong> Where is this?</p>
    //             </div>
    //           </div>
    //         </div>
    //       </div>
    //     </div>
    //   `;

    //   mediaCollection.appendChild(mediaWrapper);

    //   const likeButton = document.getElementById(`like-btn-${mediaId}`);
    //   const likeCount = document.getElementById(`like-count-${mediaId}`);
    //   likeButton.addEventListener("click", async (event) => {
    //     try {
    //       await postLike(mediaId);
    //       const newCount = Number(likeCount.innerHTML) + 1;
    //       likeCount.innerHTML = `${newCount}`;
    //     } catch (error) {
    //       deleteLike(mediaId);
    //       const newCount = Number(likeCount.innerHTML) - 1;
    //       likeCount.innerHTML = `${newCount}`;
    //     }
    //   });
    //   console.log("Media loaded");
    // }
  }
}
