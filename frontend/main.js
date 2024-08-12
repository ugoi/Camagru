function isUserLoggedIn() {
  return false;
}

function onLoad() {
  if (isUserLoggedIn()) {
    window.location.href = "http://127.0.0.1:3000/index.html";
  } else {
    window.location.href = "http://127.0.0.1:3000/login.html";
  }
}
