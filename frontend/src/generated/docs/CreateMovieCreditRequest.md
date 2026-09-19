
# CreateMovieCreditRequest


## Properties

Name | Type
------------ | -------------
`movieId` | number
`personId` | number
`roleId` | number

## Example

```typescript
import type { CreateMovieCreditRequest } from '@codelab/api'

// TODO: Update the object below with actual values
const example = {
  "movieId": null,
  "personId": null,
  "roleId": null,
} satisfies CreateMovieCreditRequest

console.log(example)

// Convert the instance to a JSON string
const exampleJSON: string = JSON.stringify(example)
console.log(exampleJSON)

// Parse the JSON string back to an object
const exampleParsed = JSON.parse(exampleJSON) as CreateMovieCreditRequest
console.log(exampleParsed)
```

[[Back to top]](#) [[Back to API list]](../README.md#api-endpoints) [[Back to Model list]](../README.md#models) [[Back to README]](../README.md)


