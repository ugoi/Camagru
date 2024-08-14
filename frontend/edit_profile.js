async function handleEditProfile() {
  event.preventDefault();
  var username = document.getElementById("username").value;
  var email = document.getElementById("email").value;
  var password = document.getElementById("password").value;

  console.log(username);

  await updateUsername(username);
  await updateEmail(email);

  if (password) await updatePassword(password);

  await handleLoadEditProfile();
}

async function handleLoadEditProfile() {
  var profile = await getProfile();

  document.getElementById("username").value = profile.username;
  document.getElementById("email").value = profile.email;
}

async function updateUsername(username) {
  console.log("updating username");

  //Make request to server
  const myHeaders = new Headers();
  myHeaders.append("Content-Type", "application/json");

  const raw = JSON.stringify({
    username: username,
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
    "http://127.0.0.1:8000/api/user/profile/username",
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

async function updateEmail(email) {
  console.log("updating email");

  //Make request to server
  const myHeaders = new Headers();
  myHeaders.append("Content-Type", "application/json");

  const raw = JSON.stringify({
    email: email,
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
    "http://127.0.0.1:8000/api/user/profile/email",
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

async function updatePassword(password) {
  console.log("updating password");

  //Make request to server
  const myHeaders = new Headers();
  myHeaders.append("Content-Type", "application/json");

  const raw = JSON.stringify({
    password: password,
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
    "http://127.0.0.1:8000/api/user/profile/password",
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

/**
 * @typedef {Object} Profile
 * @property {string} username
 * @property {string} email
 * 
 * /**


/**
 * @returns {Profile}
 */
async function getProfile() {
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
