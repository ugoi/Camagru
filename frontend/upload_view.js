import { postMedia, getMedia } from "./upload.js";

/**
 * @type {HTMLFormElement}
 * */
const mediaForm = document.getElementById("mediaForm");

/**
 * @type {HTMLFormElement}
 */
const overlayMeidaForm = document.getElementById("overlayMediaForm");

/**
 * @type {HTMLButtonElement}
 */
const submitButton = document.getElementById("submitButton");

/**
 * @type {HTMLInputElement}
 */
const fileInput = document.getElementById("fileInput");

/**
 * @type {HTMLImageElement}
 */
const mediaPreview = document.querySelector("#image");

fileInput.addEventListener("change", (event) => {
  if (event?.target?.files && event.target.files[0]) {
    console.log("loading file");
    mediaPreview.src = URL.createObjectURL(event.target.files[0]);
  }
});

const formData = new FormData(mediaForm);

submitButton.addEventListener("click", async (event) => {
  event.preventDefault();
  const overlayMediaFormData = new FormData(overlayMeidaForm);
  const mediaFormData = new FormData(mediaForm);

  var overlayMediaFile = overlayMediaFormData.get("overlayMedia");
  var mediaFile = mediaFormData.get("media");

  formData.append("overlayMedia", overlayMediaFile);
  formData.append("media", mediaFile);

  var postResponse = await postMedia(formData);

  var postJson = await postResponse.json();

  console.log("postJson");

  console.log(postJson);

  var id = postJson.containerId;

  var response = await getMedia(id);

  var mimeType = await response.headers.get("Content-type");

  console.log("mimeTypes");

  console.log(mimeType);

  var myBlob = await response.blob();

  var objectURL = URL.createObjectURL(myBlob);
  document.querySelector("#image").src = objectURL;
});
