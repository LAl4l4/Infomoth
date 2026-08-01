module.exports = {
  testEnvironment: 'jest-environment-jsdom',
  transform: {
    '^.+\\.(js|jsx|ts|tsx)$': 'babel-jest',
  },
  moduleNameMapper: {
    '\\.(css|less|scss)$': '<rootDir>/src/__jest__/styleMock.js',
    '^react-router-dom$': '<rootDir>/src/__jest__/react-router-dom.js',
    '^react-router/dom$': '<rootDir>/src/__jest__/react-router-dom-export.js',
    '^react-router$': '<rootDir>/src/__jest__/react-router.js',
  },
  setupFilesAfterEnv: ['<rootDir>/src/setupTests.ts'],
};
