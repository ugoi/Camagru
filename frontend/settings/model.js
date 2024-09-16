/**
 *
 * @param {boolean} enableEmailNotifications
 * @returns  {Promise<Response>}
 */
export async function patchSettings(enableEmailNotifications) {
  const myHeaders = new Headers();
  myHeaders.append("Content-Type", "application/json");

  const raw = JSON.stringify({
    enable_email_notifications: enableEmailNotifications ? "true" : "false",
  });

  const requestOptions = {
    credentials: "include",
    mode: "cors",
    method: "PATCH",
    headers: myHeaders,
    body: raw,
    redirect: "follow",
  };

  let response = await fetch(
    `http://camagru.com:8000/api/settings`,
    requestOptions
  );

  if (response.status === 200) {
    return response;
  } else {
    throw new Error(response.text());
  }
}

export async function getSettings() {
  const requestOptions = {
    credentials: "include",
    mode: "cors",
    method: "GET",
    redirect: "follow",
  };

  let response = await fetch(
    `http://camagru.com:8000/api/settings`,
    requestOptions
  );

  if (response.status === 200) {
    return response.json();
  } else {
    throw new Error(response.text());
  }
}
