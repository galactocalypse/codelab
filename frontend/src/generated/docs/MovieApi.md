# MovieApi

All URIs are relative to *http://localhost:8080*

| Method | HTTP request | Description |
|------------- | ------------- | -------------|
| [**create1**](MovieApi.md#create1) | **POST** /api/movies |  |
| [**getAll1**](MovieApi.md#getall1) | **GET** /api/movies |  |
| [**getById1**](MovieApi.md#getbyid1) | **GET** /api/movies/{id} |  |



## create1

> Movie create1(movie)



### Example

```ts
import {
  Configuration,
  MovieApi,
} from '@codelab/api';
import type { Create1Request } from '@codelab/api';

async function example() {
  console.log("🚀 Testing @codelab/api SDK...");
  const api = new MovieApi();

  const body = {
    // Movie
    movie: ...,
  } satisfies Create1Request;

  try {
    const data = await api.create1(body);
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
| **movie** | [Movie](Movie.md) |  | |

### Return type

[**Movie**](Movie.md)

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


## getAll1

> Array&lt;Movie&gt; getAll1()



### Example

```ts
import {
  Configuration,
  MovieApi,
} from '@codelab/api';
import type { GetAll1Request } from '@codelab/api';

async function example() {
  console.log("🚀 Testing @codelab/api SDK...");
  const api = new MovieApi();

  try {
    const data = await api.getAll1();
    console.log(data);
  } catch (error) {
    console.error(error);
  }
}

// Run the test
example().catch(console.error);
```

### Parameters

This endpoint does not need any parameter.

### Return type

[**Array&lt;Movie&gt;**](Movie.md)

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


## getById1

> GetMovieResponse getById1(id, includeCredits)



### Example

```ts
import {
  Configuration,
  MovieApi,
} from '@codelab/api';
import type { GetById1Request } from '@codelab/api';

async function example() {
  console.log("🚀 Testing @codelab/api SDK...");
  const api = new MovieApi();

  const body = {
    // number
    id: 789,
    // boolean (optional)
    includeCredits: true,
  } satisfies GetById1Request;

  try {
    const data = await api.getById1(body);
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
| **id** | `number` |  | [Defaults to `undefined`] |
| **includeCredits** | `boolean` |  | [Optional] [Defaults to `false`] |

### Return type

[**GetMovieResponse**](GetMovieResponse.md)

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

