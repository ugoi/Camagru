/**
 * Type: View
 * Handles the loading of the profile page
 * @returns {void}
 */
export async function handleLoadProfile() {
  console.log("handling get profile");
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
  console.log("getting profile");

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
    "http://127.0.0.1:8000/api/user/profile",
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
