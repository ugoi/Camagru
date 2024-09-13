// Load user media
window.addEventListener("DOMContentLoaded", async (event) => {
  handleEmailValidation(event);
});

/**
 * Handles the forgot password form submission
 * @param {Event} event
 * @returns {void}
 */
export async function handleEmailValidation(event) {
  var loginSuccessElement = document.getElementById("loginSuccess");
  var loginErrorElement = document.getElementById("loginError");

  try {
    await validateEmail();
    loginSuccessElement.textContent = "Email validated successfully";
    loginSuccessElement.style.display = "block";
    loginErrorElement.style.display = "none";
  } catch (error) {
    const message = error.message;
    loginErrorElement.textContent = message;
    loginSuccessElement.style.display = "none";
    loginErrorElement.style.display = "block";
  }
}

export async function validateEmail() {
  const urlParams = new URLSearchParams(window.location.search);
  const token = urlParams.get("token");
  const myHeaders = new Headers();
  myHeaders.append("Content-Type", "application/json");
  myHeaders.append(
    "Cookie",
    "token=eyJ0eXAiOiJKV1QiLCJhbGciOiJIUzI1NiJ9.eyJzdWIiOiIxMiIsImlzcyI6ImNhbWd1cnUiLCJleHAiOjE3Mjg0MDY3NDN9.ca7z-8v5RtLjjpAscdcUJWpbTQlbVMJH-7I8x2XitZU"
  );

  const requestOptions = {
    credentials: "include",
    mode: "cors",
    method: "POST",
    headers: myHeaders,
    redirect: "follow",
  };

  const response = await fetch(
    `http://127.0.0.1:8000/api/verify-email?token=${token}`,
    requestOptions
  );

  if (response.status === 200) {
  } else {
    const json = await response.json();
    throw new Error(json.error);
  }
}
