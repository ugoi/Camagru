async function handleLogin() {
  event.preventDefault();
  var username = document.getElementById("username").value;
  var password = document.getElementById("password").value;

  var loginSuccessElement = document.getElementById("loginSuccess");
  var loginErrorElement = document.getElementById("loginError");

  try {
    await login(username, password);
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
 * @param {string} username
 * @param {string} password
 * @returns {void}
 */
async function login(username, password) {
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
