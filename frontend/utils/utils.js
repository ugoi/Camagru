/**
 * @typedef {Promise<void> & { complete: () => void }} CompletablePromise
 */

/**
 * Creates a promise that can be manually completed before the timeout.
 *
 * @param {() => void} callback - The function to call when the timeout completes or is completed manually.
 * @param {number} delay - The time in milliseconds to wait before calling the callback.
 * @returns {CompletablePromise} A promise that can be manually completed.
 */
export function createCompletableTimeout(callback, delay) {
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

/**
     Returns the dimensions of a video asynchrounsly.
     @param {HTMLVideoElement} video Video to get dimensions from.
     @return {Promise<{width: number, height: number}>} Promise which returns the dimensions of the video in 'width' and 'height' properties.
     */
export function getVideoDimensions(video) {
  return new Promise((resolve) => {
    // place a listener on it
    video.addEventListener(
      "loadedmetadata",
      function () {
        // retrieve dimensions
        const height = this.videoHeight;
        const width = this.videoWidth;

        // send back result
        resolve({ height, width });
      },
      false
    );
  });
}

/**
 *
 * @param {Blob} blob
 * @returns {Boolean} if the blob is a video
 */
export function checkIsVideo(blob) {
  var isVideo = false;
  var mimeType = blob.type;

  if (checkIsMimeTypeVideo(mimeType)) {
    isVideo = true;
  } else {
    isVideo = false;
  }
  return isVideo;
}

export async function checkFileType(url) {
  try {
    //Make request to server
    const myHeaders = new Headers();
    myHeaders.append("Content-Type", "application/json");

    const requestOptions = {
      credentials: "include",
      mode: "cors",
      method: "GET",
      headers: myHeaders,
      redirect: "follow",
    };

    const response = await fetch(url, requestOptions);

    if (response.status === 200) {
      const contentType = response.headers.get("Content-Type");
      console.log("Content Type: ");

      console.log(contentType);

      if (checkIsMimeTypeVideo(contentType)) {
        return "video";
      } else {
        return "image";
      }
    } else {
      throw new Error(response.statusText);
    }
  } catch (error) {
    console.error("Error fetching resource:", error);
    return "unknown";
  }
}

export function checkIsMimeTypeVideo(mimeType) {
  return mimeType.includes("video") || mimeType.includes("x-matroska");
}
