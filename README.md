# Nori with Hydrus

This is nori, with the hydrus api hacked in. It is shitty code, but it werks.



Not all features are fully supported, because hydrus api works differently from most booru apis.

(No pagination, and search only returns ids)


## Usage

To use, insert your access key into "password" field, and something random into the username field (I was too lazy to change the requirement for none or both). (In hydrus: `services->review services->client api->add`)

## FAQ
It doesn't work!
* Json Exception
  * The API url should NOT end with /
* SearchResult is Null
  * Make sure you entered your access key correctly, it will not work without it
