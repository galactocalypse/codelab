# PersonApi

All URIs are relative to *http://localhost:8080*

| Method | HTTP request | Description |
|------------- | ------------- | -------------|
| [**create**](PersonApi.md#create) | **POST** /api/persons |  |
| [**getAll**](PersonApi.md#getall) | **GET** /api/persons |  |
| [**getById**](PersonApi.md#getbyid) | **GET** /api/persons/{id} |  |



## create

> Person create(person)



### Example

```ts
import {
  Configuration,
  PersonApi,
} from '@codelab/api';
import type { CreateRequest } from '@codelab/api';

async function example() {
  console.log("🚀 Testing @codelab/api SDK...");
  const api = new PersonApi();

  const body = {
    // Person
    person: ...,
  } satisfies CreateRequest;

  try {
    const data = await api.create(body);
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
| **person** | [Person](Person.md) |  | |

### Return type

[**Person**](Person.md)

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


## getAll

> Array&lt;Person&gt; getAll()



### Example

```ts
import {
  Configuration,
  PersonApi,
} from '@codelab/api';
import type { GetAllRequest } from '@codelab/api';

async function example() {
  console.log("🚀 Testing @codelab/api SDK...");
  const api = new PersonApi();

  try {
    const data = await api.getAll();
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

[**Array&lt;Person&gt;**](Person.md)

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


## getById

> Person getById(id)



### Example

```ts
import {
  Configuration,
  PersonApi,
} from '@codelab/api';
import type { GetByIdRequest } from '@codelab/api';

async function example() {
  console.log("🚀 Testing @codelab/api SDK...");
  const api = new PersonApi();

  const body = {
    // number
    id: 789,
  } satisfies GetByIdRequest;

  try {
    const data = await api.getById(body);
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

[**Person**](Person.md)

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

