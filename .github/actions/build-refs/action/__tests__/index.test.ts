import * as action from "../index"

[
    ['prefers monorepo if oe-core not specified', new Map([['oe-core', '-'], ['monorepo', 'refs/heads/edge']]), ['refs/heads/edge', true]],
    ['uses oe-core if monorepo not specified', new Map([['oe-core', 'refs/heads/main'], ['monorepo', '-']]), ['refs/heads/main', true]],
    ['prefers monorepo if both specified', new Map([['oe-core', 'refs/heads/main'], ['monorepo', 'refs/heads/edge']]), ['refs/heads/edge', true]],
].forEach(([testNameFragment, inputRefs, result]) => {
    test('authoritativeRef ${testNameFragment}', () => {
        expect(action.authoritativeRef(inputRefs)).toBe(result)
    })
}
)
