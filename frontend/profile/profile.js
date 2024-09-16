import { checkUserAuthentication } from "../services/auth-service.js";

window.addEventListener(
  "DOMContentLoaded",
  (event) => {
    console.log("DOM fully loaded and parsed");
    
    const isLoggedIn = checkUserAuthentication();
    if (!isLoggedIn) {
      window.location.href = "/login";
    }
    handleLoadProfile();
  },
  false
);

/**
 * Type: View
 * Handles the loading of the profile page
 * @returns {void}
 */
export async function handleLoadProfile() {
  var usernameElement = document.getElementById("username");
  var emailElement = document.getElementById("email");

  var profile = await getProfile();
  usernameElement.textContent = profile.username;
  emailElement.textContent = profile.email;
}

/**
 * @typedef {Object} Profile
 * @property {string} username
 * @property {string} email
 * 
 * /**


/**
 * Type: Controller
 * Sends a request to the server to get the user's profile
 * @returns {Profile}
 */
export async function getProfile() {
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
    "http://camagru.com:8000/api/user/profile",
    requestOptions
  );

  const json = await response.json();
  if (response.status === 200) {
    return json;
  } else {
    throw new Error(json.error);
  }
}
