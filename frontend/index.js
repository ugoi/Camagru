function isUserLoggedIn() {
  return true;
}

function onLoad() {
  // if (isUserLoggedIn()) {
  //   console.log("User is logged in");
  // } else {
  //   console.log("User is not logged in");
  // }
}

function handleLogout() {
  delete_cookie("username");
  delete_cookie("token");

  // Redirect to login page
  window.location.href = "/login.html";
}

function delete_cookie(name) {
  document.cookie = name + "=; Path=/; Expires=Thu, 01 Jan 1970 00:00:01 GMT;";
}
