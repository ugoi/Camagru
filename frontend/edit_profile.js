import { getProfile } from "./profile.js";

/**
 * Type: View
 * Handles the edit profile form submission
 * @returns {void}
 */
export async function handleEditProfile() {
  event.preventDefault();
  var username = document.getElementById("username").value;
  var email = document.getElementById("email").value;
  var password = document.getElementById("password").value;

  console.log(username);

  await updateUsername(username);
  await updateEmail(email);

  if (password) await updatePassword(password);

  await handleLoadEditProfile();
}

/**
 * Type: View
 * Handles the loading of the edit profile page
 * @returns {void}
 */
export async function handleLoadEditProfile() {
  var profile = await getProfile();

  document.getElementById("username").value = profile.username;
  document.getElementById("email").value = profile.email;
}

/**
 * Type: Controller
 * Sends a request to the server to update the username
 * @param {string} username
 * @returns {void}
 */
async function updateUsername(username) {
  console.log("updating username");

  //Make request to server
  const myHeaders = new Headers();
  myHeaders.append("Content-Type", "application/json");

  const raw = JSON.stringify({
    username: username,
  });

  const requestOptions = {
    credentials: "include",
    mode: "cors",
    method: "PUT",
    headers: myHeaders,
    body: raw,
    redirect: "follow",
  };

  const response = await fetch(
    "http://127.0.0.1:8000/api/user/profile/username",
    requestOptions
  );

  const json = await response.json();
  console.log(json);

  if (response.status === 200) {
    return json;
  } else {
    throw new Error(json.error);
  }
}

/**
 * Type: Controller
 * Sends a request to the server to update the email
 * @param {string} email
 * @returns {void}
 */
async function updateEmail(email) {
  console.log("updating email");

  //Make request to server
  const myHeaders = new Headers();
  myHeaders.append("Content-Type", "application/json");

  const raw = JSON.stringify({
    email: email,
  });

  const requestOptions = {
    credentials: "include",
    mode: "cors",
    method: "PUT",
    headers: myHeaders,
    body: raw,
    redirect: "follow",
  };

  const response = await fetch(
    "http://127.0.0.1:8000/api/user/profile/email",
    requestOptions
  );

  const json = await response.json();
  console.log(json);

  if (response.status === 200) {
    return json;
  } else {
    throw new Error(json.error);
  }
}

/**
 * Type: Controller
 * Sends a request to the server to update the password
 * @param {string} password
 * @returns {void}
 */
async function updatePassword(password) {
  console.log("updating password");

  //Make request to server
  const myHeaders = new Headers();
  myHeaders.append("Content-Type", "application/json");

  const raw = JSON.stringify({
    password: password,
  });

  const requestOptions = {
    credentials: "include",
    mode: "cors",
    method: "PUT",
    headers: myHeaders,
    body: raw,
    redirect: "follow",
  };

  const response = await fetch(
    "http://127.0.0.1:8000/api/user/profile/password",
    requestOptions
  );

  const json = await response.json();
  console.log(json);

  if (response.status === 200) {
    return json;
  } else {
    throw new Error(json.error);
  }
}
