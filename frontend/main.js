function isUserLoggedIn() {
  return true;
}

function onLoad() {
  if (isUserLoggedIn()) {
    window.location.href = "http://127.0.0.1:3000/index.html";
  } else {
    window.location.href = "http://127.0.0.1:3000/login.html";
  }
}

function handleLogout() {
  delete_cookie("username");
  delete_cookie("token");
}

function delete_cookie(name) {
  document.cookie = name + "=; Path=/; Expires=Thu, 01 Jan 1970 00:00:01 GMT;";
}
