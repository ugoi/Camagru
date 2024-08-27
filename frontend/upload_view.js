import { postMedia, getMedia } from "./upload.js";

const mediaForm = document.getElementById("mediaForm");
const overlayMeidaForm = document.getElementById("overlayMediaForm");
const submitButton = document.getElementById("submitButton");

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
