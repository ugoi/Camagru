/**
 * @param {String} after
 * @param {number} limit
 * @returns {Promise<Response>}
 */
export async function getUserFeed(after, limit) {
  const myHeaders = new Headers();
  myHeaders.append("Content-Type", "application/json");

  const requestOptions = {
    credentials: "include",
    mode: "cors",
    method: "GET",
    headers: myHeaders,
    redirect: "follow",
  };

  let response = await fetch(
    `http://camagru.com:8000/api/feed?after=${after}&limit=${limit}`,
    requestOptions
  );

  if (response.status === 200) {
    return response;
  } else {
    throw new Error(response.statusText);
  }
}

/**
 * @param {String} mediaId
 * @returns {Promise<Response>}
 */
export async function postLike(mediaId) {
  const myHeaders = new Headers();
  myHeaders.append("Content-Type", "application/json");

  const raw = JSON.stringify({
    reaction: "like",
    media_id: mediaId,
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
    `http://camagru.com:8000/api/likes`,
    requestOptions
  );

  if (response.status === 200) {
    return response;
  } else {
    throw new Error(await response.text());
  }
}

/**
 * @param {String} mediaId
 * @returns {Promise<Response>}
 */
export async function deleteLike(mediaId) {
  const myHeaders = new Headers();
  myHeaders.append("Content-Type", "application/json");

  const requestOptions = {
    credentials: "include",
    mode: "cors",
    method: "DELETE",
    headers: myHeaders,
    redirect: "follow",
  };

  const response = await fetch(
    `http://camagru.com:8000/api/likes?media_id=${mediaId}`,
    requestOptions
  );

  if (response.status === 200) {
    return response;
  } else {
    throw new Error(await response.text());
  }
}

/**
 * @param {String} mediaId
 * @returns {Promise<Response>}
 */
export async function getLikesCount(mediaId) {
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
    `http://camagru.com:8000/api/likes?media_id=${mediaId}`,
    requestOptions
  );

  if (response.status === 200) {
    return response;
  } else {
    throw new Error(await response.text());
  }
}

/**
 * 
 * @param {String} mediaId 
 * @param {String} commentTitle 
 * @param {String} commentBody 
 * @returns {Promise<Response>}
 * @throws {Error}
 */
export async function postComment(mediaId, commentTitle, commentBody) {
  const myHeaders = new Headers();
  myHeaders.append("Content-Type", "application/json");

  const raw = JSON.stringify({
    comment_title: commentTitle,
    comment_body: commentBody,
    media_id: mediaId,
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
    "http://camagru.com:8000/api/comments",
    requestOptions
  );

  if (response.status === 200) {
    return response;
  } else if (response.status === 413) {
    throw new Error("Comment too long");
  } else {
    throw new Error(await response.text());
  }
}

export async function getComments(mediaId) {
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
    `http://camagru.com:8000/api/comments?media_id=${mediaId}`,
    requestOptions
  );

  if (response.status === 200) {
    return response;
  } else {
    // Log reponse body
    throw new Error(await response.text());
  }
}
