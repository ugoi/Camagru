
/**
 * 
 * @param {Blob} stream
 * @returns 
 */
export async function takepicture(stream) {
  /**
   * @type {HTMLCanvasElement}
   */
  const canvas = document.createElement("canvas");

  /**
   * @type {HTMLVideoElement}
   */
  const video = document.createElement("video");
  video.style.width = "500px";
  video.srcObject = stream;
  video.play();

  var { width, height } = await getVideoDimensions(video);

  const context = canvas.getContext("2d");

  canvas.width = width;
  canvas.height = height;

  context.drawImage(video, 0, 0, width, height);

  // const data = canvas.toDataURL("image/png");

  return new Promise((resolve) => {
    canvas.toBlob((blob) => {
      resolve(blob);
    });
  });
}

export function startRecording(stream, lengthInMS) {
  let recorder = new MediaRecorder(stream);
  let data = [];

  recorder.ondataavailable = (event) => data.push(event.data);
  recorder.start();

  let stopped = new Promise((resolve, reject) => {
    recorder.onstop = resolve;
    recorder.onerror = (event) => reject(event.name);
  });

  // Usage Example:
  const timeout = createCompletableTimeout(() => {
    console.log("Timeout completed");

    if (recorder.state === "recording") {
      recorder.stop();
    }
  }, lengthInMS);

  let recorded = timeout;

  stopRecordingFunction = () => {
    console.log("about to call stopRecordingFunction");
    timeout.complete();
  };

  return Promise.all([stopped, recorded]).then(() => data);
}

// Utils
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
function createCompletableTimeout(callback, delay) {
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
function getVideoDimensions(video) {
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

  if (mimeType.startsWith("video")) {
    isVideo = true;
  } else {
    isVideo = false;
  }
  return isVideo;
}
