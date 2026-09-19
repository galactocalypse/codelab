# MovieRoleApi

All URIs are relative to *http://localhost:8080*

| Method | HTTP request | Description |
|------------- | ------------- | -------------|
| [**create2**](MovieRoleApi.md#create2) | **POST** /api/movie-roles |  |
| [**getAll2**](MovieRoleApi.md#getall2) | **GET** /api/movie-roles |  |
| [**getById2**](MovieRoleApi.md#getbyid2) | **GET** /api/movie-roles/{id} |  |



## create2

> MovieRole create2(movieRole)



### Example

```ts
import {
  Configuration,
  MovieRoleApi,
} from '@codelab/api';
import type { Create2Request } from '@codelab/api';

async function example() {
  console.log("🚀 Testing @codelab/api SDK...");
  const api = new MovieRoleApi();

  const body = {
    // MovieRole
    movieRole: ...,
  } satisfies Create2Request;

  try {
    const data = await api.create2(body);
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
| **movieRole** | [MovieRole](MovieRole.md) |  | |

### Return type

[**MovieRole**](MovieRole.md)

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


## getAll2

> Array&lt;MovieRole&gt; getAll2()



### Example

```ts
import {
  Configuration,
  MovieRoleApi,
} from '@codelab/api';
import type { GetAll2Request } from '@codelab/api';

async function example() {
  console.log("🚀 Testing @codelab/api SDK...");
  const api = new MovieRoleApi();

  try {
    const data = await api.getAll2();
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

[**Array&lt;MovieRole&gt;**](MovieRole.md)

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


## getById2

> MovieRole getById2(id)



### Example

```ts
import {
  Configuration,
  MovieRoleApi,
} from '@codelab/api';
import type { GetById2Request } from '@codelab/api';

async function example() {
  console.log("🚀 Testing @codelab/api SDK...");
  const api = new MovieRoleApi();

  const body = {
    // number
    id: 789,
  } satisfies GetById2Request;

  try {
    const data = await api.getById2(body);
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

### Return type

[**MovieRole**](MovieRole.md)

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

