const loginForm = document.querySelector("#login-form");

// listening to click events
loginForm.addEventListener("submit", (event) => {
  console.log("Start");
  event.preventDefault();
  handlePasswordReset(event);
});

/**
 * Handles the forgot password form submission
 * @param {Event} event
 * @returns {void}
 */
export async function handlePasswordReset(event) {
  console.log("Handling login form submission");

  var password = document.getElementById("password").value;

  var loginSuccessElement = document.getElementById("loginSuccess");
  var loginErrorElement = document.getElementById("loginError");

  try {
    await resetPassword(password);
    loginSuccessElement.textContent = "Password reset successfully";
    loginSuccessElement.style.display = "block";
    loginErrorElement.style.display = "none";
  } catch (error) {
    const message = "Error resetting password";
    loginErrorElement.textContent = message;
    loginSuccessElement.style.display = "none";
    loginErrorElement.style.display = "block";
  }
}

export async function resetPassword(newPassword) {
  if (newPassword === "" || newPassword === null) {
    throw new Error("Password cannot be empty");
  }

  const urlParams = new URLSearchParams(window.location.search);
  const token = urlParams.get("token");

  console.log("token", token);

  const myHeaders = new Headers();
  myHeaders.append("Content-Type", "application/json");
  myHeaders.append(
    "Cookie",
    "token=eyJ0eXAiOiJKV1QiLCJhbGciOiJIUzI1NiJ9.eyJzdWIiOiIxMiIsImlzcyI6ImNhbWd1cnUiLCJleHAiOjE3Mjg0MDY3NDN9.ca7z-8v5RtLjjpAscdcUJWpbTQlbVMJH-7I8x2XitZU"
  );

  const raw = JSON.stringify({
    password: newPassword,
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
    `http://127.0.0.1:8000/api/user/profile/password?token=${token}`,
    requestOptions
  );

  if (response.status === 200) {
  } else {
    throw new Error(json.error);
  }
}
