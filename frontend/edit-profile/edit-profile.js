import { getProfile } from "../profile/profile.js";

/**
 * @type {HTMLElement}
 */
const registerError = document.getElementById("registerError");

/**
 * Type: View
 * Handles the edit profile form submission
 * @returns {void}
 */
export async function handleEditProfile(event) {
  event.preventDefault();

  try {
    var username = document.getElementById("username").value;
    var email = document.getElementById("email").value;
    var password = document.getElementById("password").value;

    await updateUsername(username);
    await updateEmail(email);

    if (password) await updatePassword(password);

    await handleLoadEditProfile();
    window.location.href = "/profile?showSuccess=true";
  } catch (error) {
    showErrorMessage(error.message);
  }
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
  try {
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
      "http://camagru.com:8000/api/user/profile/username",
      requestOptions
    );

    const json = await response.json();

    if (response.status === 200) {
      return json;
    } else {
      throw new Error(json.error);
    }
  } catch (error) {
    throw new Error("Network error. Please check your internet connection.");
  }
}

/**
 * Type: Controller
 * Sends a request to the server to update the email
 * @param {string} email
 * @returns {void}
 */
async function updateEmail(email) {
  try {
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
      "http://camagru.com:8000/api/user/profile/email",
      requestOptions
    );

    const json = await response.json();

    if (response.status === 200) {
      return json;
    } else {
      throw new Error(json.error);
    }
  } catch (error) {
    throw new Error("Network error. Please check your internet connection.");
  }
}

/**
 * Type: Controller
 * Sends a request to the server to update the password
 * @param {string} password
 * @returns {void}
 */
async function updatePassword(password) {
  try {
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
      "http://camagru.com:8000/api/user/profile/password",
      requestOptions
    );

    const json = await response.json();

    if (response.status === 200) {
      return json;
    } else {
      throw new Error(json.error);
    }
  } catch (error) {
    throw new Error("Network error. Please check your internet connection.");
  }
}

function showSuccessMessage() {
  const successMessage = document.getElementById("successMessage");
  successMessage.classList.add("show");

  // Hide the message after 3 seconds
  setTimeout(() => {
    successMessage.classList.remove("show");
  }, 3000);
}

function showErrorMessage(message) {
  const errorMessage = document.getElementById("errorMessage");
  errorMessage.querySelector("span").textContent = message;
  errorMessage.classList.add("show");

  // Hide the message after 3 seconds
  setTimeout(() => {
    errorMessage.classList.remove("show");
  }, 3000);
}
