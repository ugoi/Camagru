/**
 * Type: View
 * Handles the registration of a new user
 * @param {Event} event
 * @returns {void}
 */
export async function handleRegister(event) {
  event.preventDefault();
  var username = document.getElementById("username").value;
  var email = document.getElementById("email").value;
  var password = document.getElementById("password").value;

  var registerSuccessElement = document.getElementById("registerSuccess");
  var registerErrorElement = document.getElementById("registerError");

  try {
    await register(username, email, password);
    registerSuccessElement.textContent = "User registered successfully!!!";
    registerSuccessElement.style.display = "block";
    registerErrorElement.style.display = "none";
  } catch (error) {
    const message = "Error registering user: " + error.message;
    registerErrorElement.textContent = message;
    registerSuccessElement.style.display = "none";
    registerErrorElement.style.display = "block";
  }
}

/**
 * Type: Controller
 * Sends a request to the server to register a new user
 * @param {string} username
 * @param {string} email
 * @param {string} password
 * @returns {void}
 */
async function register(username, email, password) {
  //Make request to server
  const myHeaders = new Headers();
  myHeaders.append("Content-Type", "application/json");

  const raw = JSON.stringify({
    username: username,
    email: email,
    password: password,
  });

  const requestOptions = {
    method: "POST",
    headers: myHeaders,
    body: raw,
    redirect: "follow",
  };

  const response = await fetch(
    "http://camagru.com:8000/api/register/",
    requestOptions
  );

  const json = await response.json();
  if (response.status === 201) {
  } else {
    throw new Error(json.error);
  }
}
