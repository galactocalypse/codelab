# MovieCreditApi

All URIs are relative to *http://localhost:8080*

| Method | HTTP request | Description |
|------------- | ------------- | -------------|
| [**create3**](MovieCreditApi.md#create3) | **POST** /api/movie-credits |  |
| [**getMovieCredits**](MovieCreditApi.md#getmoviecredits) | **GET** /api/movie-credits/by-movie/{movieId} |  |
| [**getPersonCredits**](MovieCreditApi.md#getpersoncredits) | **GET** /api/movie-credits/by-movie/{personId} |  |



## create3

> MovieCredit create3(createMovieCreditRequest)



### Example

```ts
import {
  Configuration,
  MovieCreditApi,
} from '@codelab/api';
import type { Create3Request } from '@codelab/api';

async function example() {
  console.log("🚀 Testing @codelab/api SDK...");
  const api = new MovieCreditApi();

  const body = {
    // CreateMovieCreditRequest
    createMovieCreditRequest: ...,
  } satisfies Create3Request;

  try {
    const data = await api.create3(body);
    console.log(data);
  } catch (error) {
    console.error(error);
  }
}

// Run the test
example().catch(console.error);
```

### Parameters


| Name | Type | Description  | Notes |
|------------- | ------------- | ------------- | -------------|
| **createMovieCreditRequest** | [CreateMovieCreditRequest](CreateMovieCreditRequest.md) |  | |

### Return type

[**MovieCredit**](MovieCredit.md)

### Authorization

No authorization required

### HTTP request headers

- **Content-Type**: `application/json`
- **Accept**: `*/*`


### HTTP response details
| Status code | Description | Response headers |
|-------------|-------------|------------------|
| **200** | OK |  -  |

[[Back to top]](#) [[Back to API list]](../README.md#api-endpoints) [[Back to Model list]](../README.md#models) [[Back to README]](../README.md)


## getMovieCredits

> Array&lt;MovieCredit&gt; getMovieCredits(movieId)



### Example

```ts
import {
  Configuration,
  MovieCreditApi,
} from '@codelab/api';
import type { GetMovieCreditsRequest } from '@codelab/api';

async function example() {
  console.log("🚀 Testing @codelab/api SDK...");
  const api = new MovieCreditApi();

  const body = {
    // number
    movieId: 789,
  } satisfies GetMovieCreditsRequest;

  try {
    const data = await api.getMovieCredits(body);
    console.log(data);
  } catch (error) {
    console.error(error);
  }
}

// Run the test
example().catch(console.error);
```

### Parameters


| Name | Type | Description  | Notes |
|------------- | ------------- | ------------- | -------------|
| **movieId** | `number` |  | [Defaults to `undefined`] |

### Return type

[**Array&lt;MovieCredit&gt;**](MovieCredit.md)

### Authorization

No authorization required

### HTTP request headers

- **Content-Type**: Not defined
- **Accept**: `*/*`


### HTTP response details
| Status code | Description | Response headers |
|-------------|-------------|------------------|
| **200** | OK |  -  |

[[Back to top]](#) [[Back to API list]](../README.md#api-endpoints) [[Back to Model list]](../README.md#models) [[Back to README]](../README.md)


## getPersonCredits

> Array&lt;MovieCredit&gt; getPersonCredits(personId)



### Example

```ts
import {
  Configuration,
  MovieCreditApi,
} from '@codelab/api';
import type { GetPersonCreditsRequest } from '@codelab/api';

async function example() {
  console.log("🚀 Testing @codelab/api SDK...");
  const api = new MovieCreditApi();

  const body = {
    // number
    personId: 789,
  } satisfies GetPersonCreditsRequest;

  try {
    const data = await api.getPersonCredits(body);
    console.log(data);
  } catch (error) {
    console.error(error);
  }
}

// Run the test
example().catch(console.error);
```

### Parameters


| Name | Type | Description  | Notes |
|------------- | ------------- | ------------- | -------------|
| **personId** | `number` |  | [Defaults to `undefined`] |

### Return type

[**Array&lt;MovieCredit&gt;**](MovieCredit.md)

### Authorization

No authorization required

### HTTP request headers

- **Content-Type**: Not defined
- **Accept**: `*/*`


### HTTP response details
| Status code | Description | Response headers |
|-------------|-------------|------------------|
| **200** | OK |  -  |

[[Back to top]](#) [[Back to API list]](../README.md#api-endpoints) [[Back to Model list]](../README.md#models) [[Back to README]](../README.md)

