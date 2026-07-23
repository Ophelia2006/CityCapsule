import abilityTest from './Ability.test';
import coldStartTest from './ColdStart.test';
import storageTest from './Storage.test';

export default function testsuite() {
  abilityTest();
  coldStartTest();
  storageTest();
}
