const loginForm = document.querySelector("#login-form");

// listening to click events
loginForm.addEventListener("submit", (event) => {
  event.preventDefault();
  handleLogin(event);
});

/**
 * Handles the forgot password form submission
 * @param {Event} event
 * @returns {void}
 */
export async function handleLogin(event) {
  var email = document.getElementById("email").value;

  var loginSuccessElement = document.getElementById("loginSuccess");
  var loginErrorElement = document.getElementById("loginError");

  try {
    await sendLoginRequest(email);
    loginSuccessElement.textContent = "Email sent successfully";
    loginSuccessElement.style.display = "block";
    loginErrorElement.style.display = "none";
  } catch (error) {
    const message = "Error sending email";
    loginErrorElement.textContent = message;
    loginSuccessElement.style.display = "none";
    loginErrorElement.style.display = "block";
  }
}

/**
 * Type: Modell
 * @param {string} email
 * @returns {void}
 */
async function sendLoginRequest(email) {
  //Make request to server
  const myHeaders = new Headers();
  myHeaders.append("Content-Type", "application/json");

  const requestOptions = {
    credentials: "include",
    mode: "cors",
    method: "POST",
    headers: myHeaders,
    redirect: "follow",
  };

  const response = await fetch(
    `http://camagru.com:8000/api/forgot-password?email=${email}`,
    requestOptions
  );

  if (response.status === 200) {
  } else {
    throw new Error(json.error);
  }
}
