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
  
    const response = await fetch(
      `http://127.0.0.1:8000/api/feed?after=${after}&limit=${limit}`,
      requestOptions
    );
  
    if (response.status === 200) {
      return response;
    } else {
      throw new Error(json.error);
    }
  }
