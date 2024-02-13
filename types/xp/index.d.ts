import type {Content} from '@enonic-types/core';
import type {Site} from '@enonic-types/lib-content';


export declare type BaseFolderContent = Content<{}, 'base:folder'>;

export declare type MediaImageContent = Content<{
	altText?: string
	artist?: string[]
	caption?: string
	copyright?: string
	media: {
		attachment: string
		focalPoint: {
			x: number // Float
			y: number // Float
		}
	}
	tags?: string[]
}, 'media:image'>;

export declare type PortalSiteContent<Config extends any = undefined> = Site<Config>;
