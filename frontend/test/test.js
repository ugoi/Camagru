import { getUserFeed } from "../feed/feed-model.js";

// Load user media
window.addEventListener("DOMContentLoaded", async (event) => {
  console.log("Loading feed");

  const after = null;
  const result = await getUserFeed(after, 3);
  const json = await result.json();
  const data = json.data;
  console.log(data);

  console.log("Loaded feed");
});
