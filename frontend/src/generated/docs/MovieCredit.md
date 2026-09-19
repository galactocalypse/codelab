
# MovieCredit


## Properties

Name | Type
------------ | -------------
`id` | number
`movie` | [Movie](Movie.md)
`person` | [Person](Person.md)
`role` | [MovieRole](MovieRole.md)

## Example

```typescript
import type { MovieCredit } from '@codelab/api'

// TODO: Update the object below with actual values
const example = {
  "id": null,
  "movie": null,
  "person": null,
  "role": null,
} satisfies MovieCredit

console.log(example)

// Convert the instance to a JSON string
const exampleJSON: string = JSON.stringify(example)
console.log(exampleJSON)

// Parse the JSON string back to an object
const exampleParsed = JSON.parse(exampleJSON) as MovieCredit
console.log(exampleParsed)
```

[[Back to top]](#) [[Back to API list]](../README.md#api-endpoints) [[Back to Model list]](../README.md#models) [[Back to README]](../README.md)


