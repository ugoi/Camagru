/**
 * Type: Modell
 * @param {FormData} formData
 * @returns {Response}
 */
export async function postMedia(formData) {
  //Make request to server
  const myHeaders = new Headers();

  const requestOptions = {
    credentials: "include",
    mode: "cors",
    method: "POST",
    body: formData,
    headers: myHeaders,
    redirect: "follow",
  };

  const response = await fetch(
    "http://127.0.0.1:8000/api/media?scale_factor=0.1&x_position_factor=0&y_position_factor=0",
    requestOptions
  );

  if (response.status === 200) {
    console.log(response);

    return response;
  } else {
    throw new Error(response.error);
  }
}

/**
 * Type: Modell
 * @param {String} id
 * @returns {Response}
 */
export async function getMedia(id) {
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
    `http://127.0.0.1:8000/api/serve/media?id=${id}`,
    requestOptions
  );

  if (response.status === 200) {
    return response;
  } else {
    throw new Error(json.error);
  }
}
