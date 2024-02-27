import type {
	Brand,
	BrandGraphQLObjectType,
	BrandGraphQLScalarType,
	GetSuperType,
	GetNonNull,
	GetParsedJson,
	GetReference,
	NonNull,
	Reference,
} from '../brand';

import {
	expectAssignable,
	expectType,
} from 'tsd';
import {
	jsonParse,
	jsonStringify,
} from '../advanced';


type MyString = BrandGraphQLScalarType<'MyString', string>
expectAssignable<MyString>(<MyString>'string')
expectAssignable<MyString>('string' as MyString)

// let myString: MyString;
// myString = 'string'; // Not assignable :(
// expectAssignable<MyString>(myString)

expectType<MyString>(<MyString>'string')
expectType<string>(<GetSuperType<MyString>>'string')

expectType<false>(<GetNonNull<MyString>>false)

type MyRequiredString = NonNull<MyString>

// let myRequiredString: MyRequiredString;
// myRequiredString = 'string' // Not assignable :(
// expectAssignable<MyRequiredString>(myRequiredString)

expectAssignable<MyRequiredString>(<MyRequiredString>'string')
expectAssignable<MyRequiredString>('string' as MyRequiredString)

expectType<true>(<GetNonNull<MyRequiredString>>true)

type MyRecord = BrandGraphQLObjectType<'MyRecord', Record<string,unknown>>
expectType<MyRecord>(<MyRecord>{})
expectType<Record<string,unknown>>(<GetSuperType<MyRecord>>{})
expectType<false>(<GetNonNull<MyRecord>>false)

type ReferenceToMyRecord = Reference<MyRecord>

expectType<true>(<GetReference<ReferenceToMyRecord>>true);

//──────────────────────────────────────────────────────────────────────────────
// Json
//──────────────────────────────────────────────────────────────────────────────

interface MyInterface {
	key: string
}

type MyInterfaceJson = Brand<'MyInterfaceJson', string, MyInterface>

const myInterface: MyInterface = {
	key: 'value'
}

const myInterfaceJson = jsonStringify<MyInterfaceJson>(myInterface);

const parsed = jsonParse(myInterfaceJson);

expectType<MyInterface>(parsed);
expectType<MyInterface>(<GetParsedJson<MyInterfaceJson>>myInterface);

type MyInterfaceJsonNotJson = Brand<'MyInterfaceJsonNotJson', string>
expectType<undefined>(<GetParsedJson<MyInterfaceJsonNotJson>>undefined);
