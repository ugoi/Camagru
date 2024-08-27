/**
 * Type: Controller
 * Handles the login form submission
 * @param {Event} event
 * @returns {void}
 */
export async function handleLogin(event) {
  console.log("Handling login form submission");

  event.preventDefault();
  var username = document.getElementById("username").value;
  var password = document.getElementById("password").value;

  var loginSuccessElement = document.getElementById("loginSuccess");
  var loginErrorElement = document.getElementById("loginError");

  try {
    await sendLoginRequest(username, password);
    loginSuccessElement.textContent = "User logined successfully!!!";
    loginSuccessElement.style.display = "block";
    loginErrorElement.style.display = "none";
    // Redirect to index.html
    window.location.href = "/index.html";
  } catch (error) {
    const message = "Error logining user: " + error.message;
    loginErrorElement.textContent = message;
    loginSuccessElement.style.display = "none";
    loginErrorElement.style.display = "block";
  }
}

/**
 *  Type: Model
 *  Logs out the user
 *  Delete cookies and redirect to login page
 * @returns {void}
 */
export function logout() {
  delete_cookie("username");
  delete_cookie("token");

  // Redirect to login page
  window.location.href = "/login.html";
}

/**
 * Type: Model
 * Deletes a cookie by name
 * @param {string} name
 * @returns {void}
 */
function delete_cookie(name) {
  document.cookie = name + "=; Path=/; Expires=Thu, 01 Jan 1970 00:00:01 GMT;";
}

/**
 * Type: Modell
 * @param {string} username
 * @param {string} password
 * @returns {void}
 */
async function sendLoginRequest(username, password) {
  console.log("logining", username, password);

  //Make request to server
  const myHeaders = new Headers();
  myHeaders.append("Content-Type", "application/json");

  const raw = JSON.stringify({
    username: username,
    password: password,
  });

  const requestOptions = {
    credentials: "include",
    mode: "cors",
    method: "POST",
    headers: myHeaders,
    body: raw,
    redirect: "follow",
  };

  const response = await fetch(
    "http://127.0.0.1:8000/api/login/",
    requestOptions
  );

  const json = await response.json();
  console.log(json);

  if (response.status === 201) {
  } else {
    throw new Error(json.error);
  }
}
