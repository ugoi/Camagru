/**
 *
 * @param {boolean} enableEmailNotifications
 * @returns  {Promise<Response>}
 */
export async function patchSettings(enableEmailNotifications) {
  try {
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
      const errorText = await response.text();
      throw new Error(errorText);
    }
  } catch (error) {
    throw new Error("Network error. Please check your internet connection.");
  }
}

export async function getSettings() {
  try {
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
      const errorText = await response.text();
      throw new Error(errorText);
    }
  } catch (error) {
    throw new Error("Network error. Please check your internet connection.");
  }
}
