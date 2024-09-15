class MediaService {
  /**
   * @type {() => void | null}
   * */
  #stopRecordingFunction;

  constructor() {
    this.#stopRecordingFunction = null;
  }

  /**
   *
   * @param {Blob} stream
   * @returns {Promise<Blob>}
   */
  async takepicture(stream) {
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

    return new Promise((resolve) => {
      canvas.toBlob((blob) => {
        resolve(blob);
      });
    });
  }

  /**
   * @param {MediaStream} stream
   * @param {number} lengthInMS
   * @returns {Promise<Blob[]>}
   */
  async startRecording(stream, lengthInMS) {
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
      if (recorder.state === "recording") {
        recorder.stop();
      }
    }, lengthInMS);

    let recorded = timeout;

    this.#stopRecordingFunction = () => {
      timeout.complete();
    };

    return Promise.all([stopped, recorded]).then(() => data);
  }

  /**
   * @returns {void}
   */
  stopRecording() {
    if (this.#stopRecordingFunction) {
      this.#stopRecordingFunction();
    }
  }
}

/**
 * @type {MediaService}
 */
export var mediaService = new MediaService();

//#region API Wrapper

export async function deleteMedia(id) {
  const myHeaders = new Headers();
  myHeaders.append("Content-Type", "application/json");

  const requestOptions = {
    credentials: "include",
    mode: "cors",
    method: "DELETE",
    headers: myHeaders,
    redirect: "follow",
  };

  const response = await fetch(
    `http://camagru.com:8000/api/media?id=${id}`,
    requestOptions
  );

  if (response.status === 200) {
    return response;
  } else {
    throw new Error(response.error);
  }
}

/**
 * @param {FormData} formData
 * @returns {Response}
 */
export async function postMedia(formData) {
  //Make request to server
  const myHeaders = new Headers();

  const requestOptions = {
    credentials: "include",
    mode: "cors",
    method: "POST",
    body: formData,
    headers: myHeaders,
    redirect: "follow",
  };

  const response = await fetch(
    "http://camagru.com:8000/api/media?scale_factor=0.1&x_position_factor=0&y_position_factor=0",
    requestOptions
  );

  if (response.status === 200) {
    return response;
  } else {
    throw new Error(response.error);
  }
}

export async function publishMedia(id) {
  const myHeaders = new Headers();

  const requestOptions = {
    credentials: "include",
    mode: "cors",
    method: "POST",
    headers: myHeaders,
    redirect: "follow",
  };

  const response = await fetch(
    `http://camagru.com:8000/api/media_publish?creation_id=${id}`,
    requestOptions
  );
  if (response.status === 200) {
    return response;
  } else {
    throw new Error(response.error);
  }
}

/**
 * @param {String} id
 * @returns {Promise<Response>}
 */
export async function getServeMedia(id) {
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

  const response = await fetch(
    `http://camagru.com:8000/api/serve/media?id=${id}`,
    requestOptions
  );

  if (response.status === 200) {
    return response;
  } else {
    throw new Error(json.error);
  }
}

/**
 * @param {String} after
 * @param {number} limit
 * @returns {Promise<Response>}
 */
export async function getUserMedia(after, limit) {
  const myHeaders = new Headers();
  myHeaders.append("Content-Type", "application/json");

  const requestOptions = {
    credentials: "include",
    mode: "cors",
    method: "GET",
    headers: myHeaders,
    redirect: "follow",
  };

  const response = await fetch(
    `http://camagru.com:8000/api/media?after=${after}&limit=${limit}`,
    requestOptions
  );

  if (response.status === 200) {
    return response;
  } else {
    throw new Error((await response.json()).error);
  }
}

//#endregion

//#region Utils
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
      if (contentType.includes("video")) {
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
//#endregion
